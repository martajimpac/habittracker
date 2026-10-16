import { createClient } from "@supabase/supabase-js";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const OAUTH_AUDIENCE = "https://oauth2.googleapis.com/token";

type FirebaseServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type ChallengeWebhookBody = {
  type?: string;
  record?: {
    id?: string;
    challenged_id?: string;
    challenger_id?: string;
  };
};

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function normalizePrivateKeyPem(privateKey: string): string {
  return privateKey.includes("\\n")
    ? privateKey.replace(/\\n/g, "\n")
    : privateKey;
}

function parseServiceAccount(raw: string): FirebaseServiceAccount | null {
  try {
    const parsed = JSON.parse(raw) as FirebaseServiceAccount;
    if (
      typeof parsed.project_id !== "string" ||
      typeof parsed.client_email !== "string" ||
      typeof parsed.private_key !== "string"
    ) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function pemToPkcs8(pem: string): Uint8Array {
  const b64 = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function signServiceAccountAssertion(
  serviceAccount: FirebaseServiceAccount,
): Promise<string> {
  const pem = normalizePrivateKeyPem(serviceAccount.private_key);
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(pem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const payload = base64Url(new TextEncoder().encode(JSON.stringify({
    iss: serviceAccount.client_email,
    sub: serviceAccount.client_email,
    aud: OAUTH_AUDIENCE,
    iat: now,
    exp: now + 3600,
    scope: FCM_SCOPE,
  })));
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(`${header}.${payload}`),
  );
  return `${header}.${payload}.${base64Url(new Uint8Array(signature))}`;
}

async function getFcmAccessToken(
  serviceAccount: FirebaseServiceAccount,
): Promise<string | null> {
  const assertion = await signServiceAccountAssertion(serviceAccount);

  const tokenResponse = await fetch(OAUTH_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!tokenResponse.ok) {
    console.error(
      "notify-challenge: FCM OAuth token exchange failed",
      tokenResponse.status,
    );
    return null;
  }

  const tokenJson = (await tokenResponse.json()) as { access_token?: string };
  if (typeof tokenJson.access_token !== "string") {
    console.error("notify-challenge: FCM OAuth response missing access_token");
    return null;
  }
  return tokenJson.access_token;
}

async function sendFcmDataMessage(
  projectId: string,
  accessToken: string,
  deviceToken: string,
  challengeId: string,
  challengerName: string,
): Promise<boolean> {
  const url =
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token: deviceToken,
        data: {
          type: "challenge_created",
          challengeId,
          challengerName,
        },
        android: { priority: "HIGH" },
      },
    }),
  });

  if (!response.ok) {
    console.error(
      "notify-challenge: FCM send failed",
      response.status,
      "challenge",
      challengeId,
    );
    return false;
  }
  return true;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  const expectedSecret = Deno.env.get("CHALLENGE_WEBHOOK_SECRET");
  const webhookSecret = req.headers.get("x-webhook-secret");
  if (!expectedSecret || webhookSecret !== expectedSecret) {
    console.error("notify-challenge: bad webhook secret");
    return json({ error: "Unauthorized" }, 401);
  }

  let payload: ChallengeWebhookBody;
  try {
    payload = await req.json();
  } catch {
    console.error("notify-challenge: invalid JSON body");
    return json({ error: "Bad request" }, 400);
  }

  if (payload.type !== "INSERT") {
    return json({ skipped: true }, 200);
  }

  const record = payload.record;
  const challengeId = record?.id;
  const challengedId = record?.challenged_id;
  const challengerId = record?.challenger_id;

  if (!challengeId || !challengedId || !challengerId) {
    console.error(
      "notify-challenge: INSERT missing challenge fields",
      challengeId ?? "unknown",
    );
    return json({ skipped: true }, 200);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    console.error("notify-challenge: missing Supabase environment variables");
    return json({ ok: true }, 200);
  }

  const admin = createClient(supabaseUrl, serviceRoleKey);

  const { data: profile, error: profileError } = await admin
    .from("profiles")
    .select("display_name")
    .eq("id", challengerId)
    .maybeSingle();

  if (profileError) {
    console.error(
      "notify-challenge: profile lookup failed",
      profileError.message,
      "challenge",
      challengeId,
    );
  }

  const challengerName =
    typeof profile?.display_name === "string" && profile.display_name.length > 0
      ? profile.display_name
      : "Challenges";

  const { data: tokenRows, error: tokensError } = await admin
    .from("device_tokens")
    .select("token")
    .eq("user_id", challengedId);

  if (tokensError) {
    console.error(
      "notify-challenge: device_tokens lookup failed",
      tokensError.message,
      "challenge",
      challengeId,
    );
    return json({ ok: true }, 200);
  }

  const tokens = (tokenRows ?? [])
    .map((row) => row.token)
    .filter((t): t is string => typeof t === "string" && t.length > 0);

  if (tokens.length === 0) {
    console.log("notify-challenge: no tokens", challengeId);
    return json({ ok: true }, 200);
  }

  const serviceAccountRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");
  if (!serviceAccountRaw) {
    console.error(
      "notify-challenge: FIREBASE_SERVICE_ACCOUNT not configured",
      "challenge",
      challengeId,
    );
    return json({ ok: true }, 200);
  }

  const serviceAccount = parseServiceAccount(serviceAccountRaw);
  if (!serviceAccount) {
    console.error(
      "notify-challenge: invalid FIREBASE_SERVICE_ACCOUNT",
      "challenge",
      challengeId,
    );
    return json({ ok: true }, 200);
  }

  const accessToken = await getFcmAccessToken(serviceAccount);
  if (!accessToken) {
    console.error("notify-challenge: no FCM access token", "challenge", challengeId);
    return json({ ok: true }, 200);
  }

  for (const deviceToken of tokens) {
    await sendFcmDataMessage(
      serviceAccount.project_id,
      accessToken,
      deviceToken,
      challengeId,
      challengerName,
    );
  }

  return json({ ok: true }, 200);
});
