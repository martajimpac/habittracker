# Friend request push Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** When someone sends a friend request, the addressee gets a system notification even if the app is closed, and tapping it opens the Friends tab.

**Architecture:** The app still only inserts into `friendships`. An `AFTER INSERT` trigger calls `notify-friend-request` with the same vault secret and `CHALLENGE_WEBHOOK_SECRET` already used by challenge push. The function reads `device_tokens` for `addressee_id` and sends an FCM data message. The device renders copy from `strings.xml` and opens Friends. No new Firebase secret and no new device-token table.

**Tech Stack:** Supabase Edge Functions, `pg_net`, Vault, FCM HTTP v1, Jetpack Compose, existing `MyFirebaseMessagingService`.

**Platform:** Android

---

### Task 1: Notification id

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/push/FriendPushIds.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/push/FriendPushIdsTest.kt`

- [x] Hash `"friend:" + friendshipId`, force the result into a positive int, and map `0` to `1`.
- [x] Assert it is never 0 and differs from `ChallengePushIds` for the same id.

### Task 2: Edge Function and insert trigger

**Files:**
- Create: `supabase/functions/notify-friend-request/index.ts`
- Create: `supabase/functions/notify-friend-request/deno.json`
- Modify: `supabase/config.toml`
- Create: `supabase/migrations/20261016120000_friend_request_push.sql`

- [x] `verify_jwt = false`. Require `x-webhook-secret` equal to `CHALLENGE_WEBHOOK_SECRET`.
- [x] On `INSERT` with `status = pending`, load the requester `display_name` and the addressee tokens. Send `type=friend_request`, `friendshipId`, `requesterName`.
- [x] Missing tokens or an FCM failure still returns 200. Do not log tokens or the secret.
- [x] Trigger `friendships_insert_push` reads vault secret `challenge_webhook_secret` and POSTs to `notify-friend-request`. Revoke `EXECUTE` from `public`, `anon`, and `authenticated`.

### Task 3: Device notification and Friends tab

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/core/MyFirebaseMessagingService.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtras.kt`
- Modify: `app/src/main/java/com/marta/habittracker/MainActivity.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/NavigationWrapper.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/bottom_nav_screen/BottomNavScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`
- Modify: `spec.md`
- Test: `app/src/test/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtrasTest.kt`

- [x] Title and body come from string resources. A blank sender name uses the localized fallback.
- [x] The pending intent carries the friendship id and the Friends tab.
- [x] A habit id or a challenge id wins over the friend-request extra.
- [x] If the viewer is logged out, drop the extra so Friends does not open after login.
- [x] Accepting or rejecting a request does not send a push.
