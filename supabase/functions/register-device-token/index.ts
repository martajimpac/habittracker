import { createClient } from "@supabase/supabase-js";

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, Authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      console.error("register-device-token: missing Authorization header");
      return json({ error: "Unauthorized" }, 401);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const anonKey =
      Deno.env.get("SUPABASE_ANON_KEY") ??
      Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      console.error(
        "register-device-token: missing required environment variables",
      );
      return json({ error: "Internal error" }, 500);
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const jwt = authHeader.replace(/^Bearer\s+/i, "");
    const {
      data: { user },
      error: userError,
    } = await userClient.auth.getUser(jwt);
    if (userError || !user) {
      console.error(
        "register-device-token: getUser failed",
        userError?.message ?? "no user",
      );
      return json({ error: "Unauthorized" }, 401);
    }

    let body: { token?: unknown };
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON body" }, 400);
    }

    const token = body.token;
    if (typeof token !== "string" || token.length === 0) {
      return json({ error: "Invalid token" }, 400);
    }
    if (token.length > 4096) {
      return json({ error: "Invalid token" }, 400);
    }

    const admin = createClient(supabaseUrl, serviceRoleKey);
    const { error: upsertError } = await admin.from("device_tokens").upsert(
      {
        token,
        user_id: user.id,
        updated_at: new Date().toISOString(),
      },
      { onConflict: "token" },
    );
    if (upsertError) {
      console.error("register-device-token: upsert failed", upsertError.message);
      return json({ error: "Internal error" }, 500);
    }

    return json({ ok: true }, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : "unknown";
    console.error("register-device-token: unhandled error", message);
    return json({ error: "Internal error" }, 500);
  }
});
