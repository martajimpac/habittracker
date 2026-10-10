# Challenge push + detail Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** When a challenge is created, the challenged user gets an FCM notification that opens that challenge, where they can accept or decline it; the Friends card opens the same screen.

**Architecture:** The app still only inserts into `challenges`. A Supabase Database Webhook calls the Edge Function `notify-challenge`, which sends a high-priority FCM data message to tokens stored in `device_tokens`. The device renders the notification from `strings.xml` and opens `TabChallengeDetail`. Accept/decline is an online-first update of `challenges.status`. Token writes go through `register-device-token` (service role) so a token left by a previous session on the same device can be reassigned; RLS only allows the owner to delete their row.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation type-safe routes, Hilt, Supabase PostgREST + Edge Functions, Firebase Cloud Messaging HTTP v1.

**Platform:** Android

**Spec:** `spec.md` § Challenge push + detail.

---

## File map

| Area | Files |
|---|---|
| SQL | Create `supabase/migrations/20261010180000_device_tokens.sql` |
| Edge | Create `supabase/functions/register-device-token/index.ts`, `supabase/functions/register-device-token/deno.json`, `supabase/functions/notify-challenge/index.ts`, `supabase/functions/notify-challenge/deno.json` |
| Domain | Modify `domain/repository/FriendsRepository.kt`. Create `domain/reminder/ChallengePushIds.kt` (pure Kotlin, no Android) |
| Data | Modify `data/remote/SocialRemoteDataSource.kt`, `data/repository/FriendsRepositoryImpl.kt`, `data/repository/AuthRepositoryImpl.kt` |
| Core | Modify `core/MyFirebaseMessagingService.kt`. Create `core/push/DeviceTokenRegistrar.kt` |
| Presentation | Modify `presentation/widgets/WidgetLaunchExtras.kt`, `MainActivity.kt`, `presentation/navigation/TabScreens.kt`, `presentation/navigation/NavigationWrapper.kt`, `presentation/navigation/NavigationBottomWrapper.kt`, `presentation/screens/bottom_nav_screen/BottomNavScreen.kt`, `presentation/screens/friends/FriendsScreen.kt`, `presentation/navigation/AppStartViewModel.kt`, login and register success paths. Create `presentation/screens/challenge/ChallengeDetailViewModel.kt`, `ChallengeDetailScreen.kt` |
| Strings | Modify `res/values/strings.xml`, `res/values-es/strings.xml` |
| Tests | Create `core/push/ChallengePushIdsTest.kt`, `presentation/screens/challenge/ChallengeDetailViewModelTest.kt`. Modify `domain/usecase/FakeFriendsRepository.kt` |

`ChallengePushIds` lives in `domain` only if it stays free of Android. Put it in `com.marta.habittracker.domain.push` so unit tests do not need Robolectric. `HabitReminderIds` stays as it is.

## Dependency graph

Do not parallelize tasks that edit the same file.

```text
Task 1 ChallengePushIds ──┐
Task 2 repository contract ┼─► Task 3 detail ViewModel
Task 4 SQL migration ──────┼─► Task 5 Edge Functions ─► Task 6 token registrar
                           └─► Task 7 data impl ───────► Task 8 navigation + UI
Task 1 ───────────────────────────────────────────────► Task 9 FCM + intent
```

Safe parallel pairs: (1, 2, 4) then (3, 5) then 7 then (6 and 9, different files) then 8 last because it wires the screen to navigation and Friends.

---

### Task 1: Notification id

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/push/ChallengePushIds.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/push/ChallengePushIdsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.marta.habittracker.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChallengePushIdsTest {
    @Test
    fun `notification id is never zero`() {
        assertNotEquals(0, ChallengePushIds.notificationIdFor(""))
        assertNotEquals(0, ChallengePushIds.notificationIdFor("challenge-1"))
    }

    @Test
    fun `same challenge id is stable and distinct from the raw habit-style hash`() {
        val id = ChallengePushIds.notificationIdFor("abc")
        assertEquals(id, ChallengePushIds.notificationIdFor("abc"))
        val habitStyle = ("abc".hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
        assertNotEquals(habitStyle, id)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.domain.push.ChallengePushIdsTest"`

Expected: FAIL, `ChallengePushIds` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.marta.habittracker.domain.push

object ChallengePushIds {
    fun notificationIdFor(challengeId: String): Int =
        (("challenge:$challengeId").hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.domain.push.ChallengePushIdsTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/domain/push/ChallengePushIds.kt app/src/test/java/com/marta/habittracker/domain/push/ChallengePushIdsTest.kt
git commit -m "feat: stable notification id for challenge pushes"
```

---

### Task 2: Repository contract

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/domain/repository/FriendsRepository.kt`
- Modify: `app/src/test/java/com/marta/habittracker/domain/usecase/FakeFriendsRepository.kt`

- [ ] **Step 1: Add the two methods to the interface**

```kotlin
suspend fun getChallenge(challengeId: String): DataResult<ChallengeCard, AppError>

suspend fun respondToChallenge(challengeId: String, accept: Boolean): DataResult<ChallengeCard, AppError>
```

`respondToChallenge` returns the updated card so the detail screen can stay on Accept without a second fetch. Decline still uses the success to navigate back.

- [ ] **Step 2: Extend the fake**

Add constructor params `getChallengeResult` and `respondToChallengeResult`, both `DataResult<ChallengeCard, AppError>`. Record `lastRespondAccept: Boolean?` and `respondCalls`. Default `getChallengeResult` to `DataResult.Error(AppError.Common.Unknown)` so existing callers that do not care still compile. Implement both overrides.

- [ ] **Step 3: Compile unit tests**

Run: `./gradlew :app:compileDebugUnitTestKotlin`

Expected: PASS. Every `FriendsRepository` fake in tests must implement the new methods. Search for `: FriendsRepository` and update each one the same way as `FakeFriendsRepository`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/domain/repository/FriendsRepository.kt app/src/test/java/com/marta/habittracker/domain/usecase/FakeFriendsRepository.kt
git commit -m "feat: add challenge detail and respond to the friends contract"
```

---

### Task 3: Challenge detail ViewModel

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/presentation/screens/challenge/ChallengeDetailViewModel.kt`
- Test: `app/src/test/java/com/marta/habittracker/presentation/screens/challenge/ChallengeDetailViewModelTest.kt`

Use `Channel(Channel.BUFFERED)` + `receiveAsFlow()` for `NavigateBack`. Follow an existing ViewModel test that uses `runTest` and `UnconfinedTestDispatcher` (see `HabitDetailViewModelTest`).

- [ ] **Step 1: Write the failing tests**

Build a `ChallengeCard` fixture whose `challenge.challengedId` is `"me"` and `challenge.challengerId` is `"them"`, status `Pending`.

Cases:

1. Load success for the challenged user sets `canRespond = true` and clears loading.
2. Load success when `getCurrentUserId()` is the challenger sets `canRespond = false`.
3. `onAccept` success replaces the card with status `Active` and `canRespond = false`. `events` is empty.
4. `onDecline` success emits `NavigateBack`.
5. `onDecline` when the fake returns `DataResult.Error(AppError.Common.Network)` sets `errorRes = R.string.error_no_internet` and does not emit `NavigateBack`.
6. `onAccept` when the viewer is the challenger does not call `respondToChallenge` (`respondCalls` stays 0).

The ViewModel reads the user id from `AuthRepository.getCurrentUserId()`. Use a tiny fake auth, not the real Supabase client.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.screens.challenge.ChallengeDetailViewModelTest"`

Expected: FAIL, unresolved ViewModel.

- [ ] **Step 3: Implement the ViewModel**

```kotlin
data class ChallengeDetailUiState(
    val isLoading: Boolean = true,
    val card: ChallengeCard? = null,
    @StringRes val errorRes: Int? = null,
    val canRespond: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface ChallengeDetailEvent {
    data object NavigateBack : ChallengeDetailEvent
}
```

`init` calls `getChallenge`. `canRespond` is `card.challenge.status == Pending && card.challenge.challengedId == userId`. `onAccept` / `onDecline` return immediately when `!canRespond || isSubmitting`. On success of accept, copy the returned card into state. On success of decline, send `NavigateBack`. On error, `errorRes = error.toStringRes()` using the same `when` as `FriendsViewModel` (`Network` → `R.string.error_no_internet`, else `R.string.challenge_respond_error`). Load failure uses `R.string.challenge_detail_error` for unknown and `R.string.error_no_internet` for network. Add those two strings in Task 8 before this test can resolve `R`; if Task 3 runs first, add only these three strings in `values/strings.xml` and `values-es/strings.xml` here:

- `challenge_detail_error` — "Couldn't load this challenge" / "No se ha podido abrir el reto"
- `challenge_respond_error` — "Couldn't update this challenge" / "No se ha podido actualizar el reto"
- `error_no_internet` already exists. Do not duplicate it.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.screens.challenge.ChallengeDetailViewModelTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/presentation/screens/challenge app/src/test/java/com/marta/habittracker/presentation/screens/challenge app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat: challenge detail view model accept and decline"
```

---

### Task 4: device_tokens migration

**Files:**
- Create: `supabase/migrations/20261010180000_device_tokens.sql`

- [ ] **Step 1: Write the migration**

```sql
create table public.device_tokens (
  token text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  updated_at timestamptz not null default now()
);

create index device_tokens_user_idx on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;

grant delete on public.device_tokens to authenticated;

create policy device_tokens_delete_own on public.device_tokens
  for delete to authenticated
  using ((select auth.uid()) = user_id);
```

No `select` / `insert` / `update` grant. The Edge Functions use the service role, which bypasses RLS.

- [ ] **Step 2: Apply it to the linked Supabase project with the project's usual migration path** (`supabase db push` or the SQL editor if that is how `20261008140000_profile_avatar_and_storage.sql` was applied). Do not invent a second migration tool.

- [ ] **Step 3: Commit**

```bash
git add supabase/migrations/20261010180000_device_tokens.sql
git commit -m "feat: add device token table for challenge push"
```

---

### Task 5: Edge Functions

**Files:**
- Create: `supabase/functions/register-device-token/index.ts`
- Create: `supabase/functions/register-device-token/deno.json`
- Create: `supabase/functions/notify-challenge/index.ts`
- Create: `supabase/functions/notify-challenge/deno.json`

`deno.json` matches `delete-account`:

```json
{
  "imports": {
    "@supabase/supabase-js": "https://esm.sh/@supabase/supabase-js@2.49.1"
  }
}
```

- [ ] **Step 1: `register-device-token`**

Copy the CORS and JWT check from `supabase/functions/delete-account/index.ts` through `getUser`. Body: `{ "token": "<fcm token>" }`. Reject empty or tokens longer than 4096 characters with 400. With the service-role client:

```typescript
const { error } = await admin.from("device_tokens").upsert(
  { token, user_id: user.id, updated_at: new Date().toISOString() },
  { onConflict: "token" },
);
```

Log `register-device-token: upsert failed` plus `error.message`. Do not log `token`. Return `{ ok: true }` with 200.

- [ ] **Step 2: `notify-challenge`**

Require header `x-webhook-secret` equal to `Deno.env.get("CHALLENGE_WEBHOOK_SECRET")`. Mismatch → 401, log `notify-challenge: bad webhook secret` without the header value.

Parse the Supabase Database Webhook body. Use `record.id`, `record.challenged_id`, `record.challenger_id`. Ignore non-INSERT payloads with 200 `{ skipped: true }`.

Service role:

1. `profiles.select("display_name").eq("id", challenger_id).maybeSingle()`
2. `device_tokens.select("token").eq("user_id", challenged_id)`

If there are no tokens, log `notify-challenge: no tokens` with the challenge id and return 200. Do not fail the webhook.

Send one FCM HTTP v1 request per token. OAuth: build an RS256 JWT from `FIREBASE_SERVICE_ACCOUNT` (JSON with `project_id`, `client_email`, `private_key`), scope `https://www.googleapis.com/auth/firebase.messaging`, audience `https://oauth2.googleapis.com/token`, then exchange it at that token URL with `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer`.

Message:

```json
{
  "message": {
    "token": "<device token>",
    "data": {
      "type": "challenge_created",
      "challengeId": "<id>",
      "challengerName": "<display name or Challenges>"
    },
    "android": { "priority": "HIGH" }
  }
}
```

POST `https://fcm.googleapis.com/v1/projects/<project_id>/messages:send`. A non-2xx from FCM is `console.error` with the status code and challenge id, not the token, and the function still returns 200 so Supabase does not retry forever. Do not log the service account.

- [ ] **Step 3: Deploy both functions and set secrets**

`CHALLENGE_WEBHOOK_SECRET` (random), `FIREBASE_SERVICE_ACCOUNT` (the Firebase service-account JSON). `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` are already injected.

- [ ] **Step 4: Create the Database Webhook in the Supabase dashboard**

- Table `public.challenges`, event Insert
- POST `{SUPABASE_URL}/functions/v1/notify-challenge`
- Header `x-webhook-secret`: the same value as the secret
- Header `Content-Type`: `application/json`

Do not commit the secret or the project URL.

- [ ] **Step 5: Commit the function source only**

```bash
git add supabase/functions/register-device-token supabase/functions/notify-challenge
git commit -m "feat: register device tokens and notify on challenge insert"
```

---

### Task 6: Register and clear the device token

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/core/push/DeviceTokenRegistrar.kt`
- Modify: `app/src/main/java/com/marta/habittracker/data/remote/SocialRemoteDataSource.kt`
- Modify: `app/src/main/java/com/marta/habittracker/data/repository/AuthRepositoryImpl.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/AppStartViewModel.kt`
- Modify: login and register ViewModels where navigation to Home happens after a successful auth result

Do not add `kotlinx-coroutines-play-services`. Bridge the Firebase task with `suspendCancellableCoroutine`.

- [ ] **Step 1: Remote methods**

On `SocialRemoteDataSource`:

```kotlin
suspend fun registerDeviceToken(token: String) {
    Log.d(TAG, "registerDeviceToken started")
    supabase.functions.invoke(FUNCTION_REGISTER_DEVICE_TOKEN, body = mapOf("token" to token))
    Log.d(TAG, "registerDeviceToken finished")
}

suspend fun deleteDeviceToken(token: String) {
    Log.d(TAG, "deleteDeviceToken started")
    supabase.from("device_tokens").delete {
        filter { eq("token", token) }
    }
    Log.d(TAG, "deleteDeviceToken finished")
}
```

`FUNCTION_REGISTER_DEVICE_TOKEN = "register-device-token"`. On failure, `Log.e(TAG, "... failed", e)` and rethrow. Never log `token`.

- [ ] **Step 2: Registrar**

`DeviceTokenRegistrar` is a `@Singleton` injected with `SocialRemoteDataSource` and `@ApplicationContext`. `registerCurrentToken()` reads `FirebaseMessaging.getInstance().token` and calls `registerDeviceToken`. `clearCurrentToken()` reads the current token and calls `deleteDeviceToken`. Both catch `Exception`, log it, and do not throw. `CancellationException` is rethrown.

- [ ] **Step 3: Call sites**

- `AppStartViewModel`: inside the existing `if (isLoggedIn)` block, call `registerCurrentToken()` next to `syncHabits()`.
- Login and register: after `DataResult.Success`, before the navigate-home event, call `registerCurrentToken()`.
- `AuthRepositoryImpl.signOut`: call `clearCurrentToken()` before `supabase.auth.signOut()`. If clear throws, log and still sign out.

Inject `DeviceTokenRegistrar` into `AuthRepositoryImpl`. That keeps every sign-out path (Profile and any other caller) clearing the token while the session is still valid.

- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/core/push/DeviceTokenRegistrar.kt app/src/main/java/com/marta/habittracker/data app/src/main/java/com/marta/habittracker/presentation/navigation/AppStartViewModel.kt app/src/main/java/com/marta/habittracker/presentation/screens/auth
git commit -m "feat: register and clear the FCM device token with the session"
```

---

### Task 7: Load and respond in the data layer

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/data/remote/SocialRemoteDataSource.kt`
- Modify: `app/src/main/java/com/marta/habittracker/data/repository/FriendsRepositoryImpl.kt`

- [ ] **Step 1: Remote**

`fetchChallengeById(id): ChallengeDto?` — `select` where `id` equals, `decodeSingleOrNull`. Log start/finish/failure. Do not log profile fields beyond the id.

`updateChallengeStatus(id, status): ChallengeDto` — same shape as `updateFriendshipStatus`, table `challenges`, body `ChallengeStatusUpdateDto(status)`.

- [ ] **Step 2: Repository**

`getChallenge` uses `runOnline`. Load the one challenge. If null, return `DataResult.Error(AppError.Common.Unknown)`. Reuse the mapping already in `getActiveChallenges` by extracting a private `toCard(challenge, userId, profilesById, habits, today): ChallengeCard?`. If the card is null (missing habit or profile), return `Unknown` and `Log.w` with the challenge id.

`respondToChallenge` uses `runOnline`. Load the challenge first. If `challengedId != requireUserId()` or status is not `pending`, return `Unknown` and do not update. Otherwise update to `active` or `declined`, then `getChallenge` logic to build the returned card. `declined` still returns the card; the ViewModel navigates away.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/data/remote/SocialRemoteDataSource.kt app/src/main/java/com/marta/habittracker/data/repository/FriendsRepositoryImpl.kt
git commit -m "feat: load one challenge and accept or decline it"
```

---

### Task 8: Detail screen, route, and Friends card

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/TabScreens.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/NavigationBottomWrapper.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/bottom_nav_screen/BottomNavScreen.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/friends/FriendsScreen.kt`
- Create: `app/src/main/java/com/marta/habittracker/presentation/screens/challenge/ChallengeDetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Route**

```kotlin
@Serializable
data class TabChallengeDetail(val challengeId: String) : TabScreens()
```

Register it in `NavigationBottomWrapper` like `TabDetail`: `ChallengeDetailScreen(onBack = { navHostController.popBackStack() })`.

In `BottomNavScreen.hideBottomBar`, also hide when the route contains `TabScreens.TabChallengeDetail`.

`FriendsScreen` takes `onOpenChallenge: (String) -> Unit`. The card's `Column` gets `Modifier.clickable { onOpenChallenge(card.challenge.id) }`. `NavigationBottomWrapper` passes `{ id -> navHostController.navigate(TabScreens.TabChallengeDetail(id)) }`.

- [ ] **Step 2: Screen**

`ChallengeDetailScreen` collects state and uses the shared `ObserveAsEvents`. `NavigateBack` calls `onBack`.

`ChallengeDetailContent(state, onBack, onAccept, onDecline)`:

- Top app bar title `R.string.challenge_detail_title`, back calls `onBack`.
- Loading: `HabitLoadingIndicator`.
- `errorRes` with no card: the string.
- Card: habit name, criteria label (reuse `friends_criteria_streak` / `friends_criteria_all_days` / `friends_criteria_completion`), both progresses, days left (`friends_days_left`), status label.
- If `canRespond`: Accept and Decline buttons. Disabled while `isSubmitting`.

Strings (EN / ES):

| name | EN | ES |
|---|---|---|
| `challenge_detail_title` | Challenge | Reto |
| `challenge_accept` | Accept | Aceptar |
| `challenge_decline` | Decline | Rechazar |
| `challenge_status_pending` | Pending | Pendiente |
| `challenge_status_active` | Active | En curso |
| `challenge_status_completed` | Completed | Completado |
| `challenge_status_declined` | Declined | Rechazado |
| `challenge_status_cancelled` | Cancelled | Cancelado |
| `challenge_push_title` | New challenge | Nuevo reto |
| `challenge_push_body` | %1$s challenged you | %1$s te ha retado |

`challenge_detail_error` and `challenge_respond_error` are added in Task 3 if that task landed first. Do not add a second copy.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/presentation app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat: open challenge detail from Friends"
```

---

### Task 9: Show the notification and open the detail

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtras.kt`
- Modify: `app/src/main/java/com/marta/habittracker/MainActivity.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/NavigationWrapper.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/bottom_nav_screen/BottomNavScreen.kt`
- Modify: `app/src/main/java/com/marta/habittracker/core/MyFirebaseMessagingService.kt`
- Test: `app/src/test/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtrasTest.kt` if it asserts the extra keys exhaustively

- [ ] **Step 1: Launch extra**

Add `EXTRA_CHALLENGE_ID = "extra_challenge_id"`.

```kotlin
fun openChallengeIntent(context: Context, challengeId: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_CHALLENGE_ID, challengeId)
    }

fun challengeIdFromIntent(intent: Intent?): String? =
    intent?.getStringExtra(EXTRA_CHALLENGE_ID)?.takeIf { it.isNotBlank() }
```

- [ ] **Step 2: Activity and navigation**

`MainActivity` holds `initialChallengeId`. `applyLaunchExtras` reads it. `consumeChallengeId()` nulls the state and removes the extra.

`NavigationWrapper` gains `initialChallengeId` and `onChallengeIdConsumed`. Pass them into `BottomNavScreen` only when `startDestination` is `Home`. When the resolved destination is `Login` or `Onboarding`, call `onChallengeIdConsumed()` once so a later login does not open the challenge.

`BottomNavScreen` navigates to `TabScreens.TabChallengeDetail(id)` the same way it navigates to `TabDetail` for a habit id, and consumes the id. If both a habit id and a challenge id are present, the challenge id wins only when the habit id is null. Notification intents do not set a habit id.

- [ ] **Step 3: Messaging service**

In `onMessageReceived`, if `data["type"] == "challenge_created"` and `data["challengeId"]` is non-blank:

- Title `getString(R.string.challenge_push_title)`
- Body `getString(R.string.challenge_push_body, data["challengerName"].orEmpty().ifBlank { getString(R.string.challenge_detail_title) })`
- `PendingIntent.getActivity` with `WidgetLaunchExtras.openChallengeIntent(this, challengeId)` and `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`
- `notify(ChallengePushIds.notificationIdFor(challengeId), ...)`
- Keep the existing channel `habit_tracker_notifications`

Leave the current `remoteMessage.notification` branch for any other FCM payload. Do not log the data map if it could contain a token; log `type` and `challengeId` only.

`onRegistered` must not log the token. Call `DeviceTokenRegistrar.registerCurrentToken()` from a coroutine scope that the service can launch (`CoroutineScope(SupervisorJob() + Dispatchers.IO)` cancelled in `onDestroy`), and do not log the token string. Inject the registrar with `@AndroidEntryPoint` on the service plus an `@EntryPoint` if Hilt cannot inject a `FirebaseMessagingService` directly. This project already uses `EntryPointAccessors` for Glance; use that same pattern here:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceTokenRegistrarEntryPoint {
    fun deviceTokenRegistrar(): DeviceTokenRegistrar
}
```

- [ ] **Step 4: Tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.widgets.WidgetLaunchExtrasTest" --tests "com.marta.habittracker.domain.push.ChallengePushIdsTest"`

Expected: PASS

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/marta/habittracker/core/MyFirebaseMessagingService.kt app/src/main/java/com/marta/habittracker/core/push app/src/main/java/com/marta/habittracker/MainActivity.kt app/src/main/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtras.kt app/src/main/java/com/marta/habittracker/presentation/navigation/NavigationWrapper.kt app/src/main/java/com/marta/habittracker/presentation/screens/bottom_nav_screen/BottomNavScreen.kt app/src/test/java/com/marta/habittracker/presentation/widgets/WidgetLaunchExtrasTest.kt
git commit -m "feat: open the challenged user on the new challenge"
```

---

### Task 10: Verification

- [ ] **Step 1: Unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.domain.push.ChallengePushIdsTest" --tests "com.marta.habittracker.presentation.screens.challenge.ChallengeDetailViewModelTest"`

Expected: PASS

- [ ] **Step 2: Debug assemble**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual check, two accounts**

1. Account A and account B are friends. B has opened the app once after this change and allowed notifications.
2. A creates a challenge against B. B receives «A te ha retado» with the app closed.
3. Tap opens that challenge. Accept leaves B on the screen without the buttons. The card in Friends shows the challenge as active after returning.
4. Repeat with Decline: the screen closes and the challenge disappears from Friends.
5. A opens the same card from Friends and does not see Accept or Decline.

---

## Spec coverage

| Requirement | Task |
|---|---|
| Push on insert, app may be closed | 5, 9 |
| Copy from strings, EN + ES | 3, 8, 9 |
| Tap opens that challenge | 9 |
| Friends card opens the same screen | 8 |
| Accept stays, Decline pops | 3, 8 |
| Challenger is read-only | 3, 7 |
| Network and missing challenge errors | 3, 7 |
| Logged-out tap drops the id | 9 |
| No push back to the challenger | out of scope, no task |
| Token table, register function, delete on sign-out | 4, 5, 6 |
| Webhook secret and FCM service account stay off the client | 5 |
| Bottom bar hidden | 8 |
| Notification id prefixed and never 0 | 1, 9 |

## Self-review

- No second network check in the ViewModel. `runOnline` in the repository returns `Network`. The ViewModel only reacts to that result.
- `respondToChallenge` returns a card so Accept does not depend on a second request.
- Token reassignment is the Edge Function, not a client upsert, because RLS cannot update another user's row.
