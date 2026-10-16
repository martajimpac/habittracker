# Preferences, avatar, and delete-account Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Ship a Preferences screen (from Profile) to edit `display_name`, set/change/remove a gallery avatar, and delete the account via a `DELETE` confirmation screen.

**Architecture:** Own-profile reads/writes go through a new `ProfileRepository` (Supabase `profiles` + Storage). Display name and `avatar_url` are the source of truth for Profile, Home, and Friends — not Auth `user_metadata`. Account deletion is an Edge Function with the service role (client cannot delete `auth.users`). UI is Screen/Content + `Channel`/`ObserveAsEvents`. Writes stay online-first.

**Tech Stack:** Jetpack Compose, Hilt, Supabase Postgrest + Storage + Auth, Photo Picker (`PickVisualMedia`), Coil (https avatars), Edge Function

**Platform:** Android

---

## File map

| Area | Paths |
|------|--------|
| Spec | `spec.md` (already updated) |
| SQL | `supabase/migrations/20261008140000_profile_avatar_and_storage.sql` |
| Edge Function | `supabase/functions/delete-account/index.ts` |
| Domain | `Profile.kt`, `ProfileRepository.kt`, `UpdateDisplayName.kt`, `DeleteAccount.kt` |
| Data | `ProfileDto`, `SocialMapper`, `SocialRemoteDataSource`, `ProfileRepositoryImpl` |
| DI | `DataModule.kt` |
| Gradle | `libs.versions.toml`, `app/build.gradle.kts` (Coil) |
| Nav | `TabScreens.kt`, `NavigationBottomWrapper.kt` |
| UI | `PreferencesScreen.kt`, `DeleteAccountScreen.kt`, `ProfileScreen.kt`, `HomeScreen` avatar, `FriendsScreen` Avatar |
| Tests | mapper, use cases, ViewModels, FakeProfileRepository |
| Strings | `values/strings.xml`, `values-es/strings.xml` |

**Out of scope:** username, camera, notifications, password change, widgets, register Name → Auth metadata.

**Order:** Task 1 (schema) → 2 (domain/data name) → 3 (Preferences name UI) → 4 (avatar upload + Coil) → 5 (show avatars) → 6 (delete function + screen). Do not parallelize 2–3 or 4–5 (same files).

---

### Task 1: Migration `avatar_url` + Storage bucket

**Files:**
- Create: `supabase/migrations/20261008140000_profile_avatar_and_storage.sql`

- [ ] **Step 1: Write migration**

```sql
alter table public.profiles
  add column if not exists avatar_url text;

insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

create policy avatars_select_authenticated
  on storage.objects for select to authenticated
  using (bucket_id = 'avatars');

create policy avatars_insert_own
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'avatars'
    and split_part(name, '/', 1) = (select auth.uid()::text)
  );

create policy avatars_update_own
  on storage.objects for update to authenticated
  using (
    bucket_id = 'avatars'
    and split_part(name, '/', 1) = (select auth.uid()::text)
  );

create policy avatars_delete_own
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'avatars'
    and split_part(name, '/', 1) = (select auth.uid()::text)
  );
```

Apply on the linked Supabase project (`apply_migration` or CLI). Confirm `profiles.avatar_url` exists.

---

### Task 2: Domain + remote profile read/update display name (TDD)

**Files:**
- Modify: `Profile.kt`, `ProfileDto.kt`, `SocialMapper.kt`, `SocialMapperTest.kt`
- Create: `domain/repository/ProfileRepository.kt`, `data/repository/ProfileRepositoryImpl.kt`
- Modify: `SocialRemoteDataSource.kt`, `AuthRepository` (+ impl) `getCurrentUserId(): String?`
- Modify: `DataModule.kt` bind `ProfileRepository`
- Test: `UpdateDisplayNameTest.kt`, `FakeProfileRepository.kt`

- [ ] **Step 1: Failing mapper test** — `ProfileDto.avatarUrl` maps to `Profile.avatarUrl` (nullable).

- [ ] **Step 2: Implement DTO/domain/mapper**

```kotlin
data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarColor: String,
    val avatarUrl: String? = null,
)
```

`ProfileDto`: `@SerialName("avatar_url") val avatarUrl: String? = null`

Update every `Profile(` test fixture with default `avatarUrl = null`.

- [ ] **Step 3: Failing `UpdateDisplayName` tests**

```kotlin
@Test
fun `blank name is invalid without calling repository`() = runTest {
    val repo = FakeProfileRepository()
    val useCase = UpdateDisplayName(repo, FakeNetworkChecker(online = true))
    val result = useCase("   ")
    assertTrue(result is DataResult.Error)
}

@Test
fun `online valid name updates repository`() = runTest { /* ... */ }

@Test
fun `offline does not call repository`() = runTest { /* Network error, repo.updateCount == 0 */ }
```

Validation: `trim()`, not blank, length in `1..40`.

- [ ] **Step 4: `ProfileRepository` + remote**

```kotlin
interface ProfileRepository {
    suspend fun getMyProfile(): DataResult<Profile, AppError>
    suspend fun updateDisplayName(displayName: String): DataResult<Profile, AppError>
    suspend fun uploadAvatar(jpegBytes: ByteArray): DataResult<Profile, AppError>
    suspend fun clearAvatar(): DataResult<Profile, AppError>
    suspend fun deleteAccount(): DataResult<Unit, AppError>
}
```

`SocialRemoteDataSource`:
- `fetchProfileById(id)` (reuse fetchProfilesByIds)
- `updateMyProfile(displayName: String?, avatarUrl: String?)` PATCH `profiles` where id = auth uid
- Storage upload/remove in Task 4; stub `uploadAvatar`/`clearAvatar`/`deleteAccount` as `TODO` **only if** Kotlin requires; prefer empty `NotImplemented` Error until Task 4/6, or implement no-ops that return Unknown — **do not ship TODO**. Until Task 4, implement `uploadAvatar`/`clearAvatar` as `DataResult.Error(AppError.Common.Unknown)` unused by UI. Task 6 implements `deleteAccount`.

`getMyProfile`: `auth.currentUserOrNull()?.id` → fetch profile. Missing user → `Unauthorized`.

Online-first in use case or repository (same pattern as SaveHabit: `NetworkChecker` first).

- [ ] **Step 5: Bind Hilt. Run unit tests.**

---

### Task 3: Preferences UI — display name + Profile/Home read `profiles`

**Files:**
- Create: `presentation/screens/preferences/PreferencesScreen.kt`, `PreferencesViewModel.kt`
- Modify: `TabScreens.kt` add `@Serializable data object TabPreferences`
- Modify: `NavigationBottomWrapper.kt`, `BottomNavScreen.kt` (hide bottom bar on Preferences like AddContent/Detail)
- Modify: `ProfileScreen.kt` (`onPreferencesClick`)
- Modify: `ProfileViewModel.kt`, `HomeViewModel.kt` to load display name via `ProfileRepository.getMyProfile()`
- Strings EN/ES
- Test: `PreferencesViewModelTest.kt`, update `ProfileViewModelTest`

- [ ] **Step 1: Failing ViewModel tests** — load prefills name; save blank sets error string res; save success sends `NavigateBack`.

- [ ] **Step 2: ViewModel** — `Channel` events: `NavigateBack`, `ShowError(@StringRes)`. State: name field, loading, avatarUrl (unused until Task 4).

- [ ] **Step 3: UI** — Scaffold/Column, back, title Preferences, `CustomTextField` display name, Save `HabitButton`. `ObserveAsEvents`. Photo row placeholder (initials only) until Task 4 — **or skip placeholder and add photo in Task 4** (prefer add photo UI in Task 4 only).

- [ ] **Step 4: Navigation** — `ProfileContent` Preferences row `onClick = onPreferences`. `composable<TabPreferences>`. Hide bottom nav for Preferences route.

- [ ] **Step 5: Profile + Home** — replace `getCurrentUserDisplayName()` with `getMyProfile().displayName` (fallback email local-part / `user_fallback_name` if error). Reload when screen starts (`LaunchedEffect` / existing Home load). Compile + tests.

---

### Task 4: Avatar upload / remove (TDD + Photo Picker)

**Files:**
- Modify: `ProfileRepositoryImpl`, `SocialRemoteDataSource` (Storage)
- Modify: `PreferencesScreen.kt` / ViewModel
- Gradle: Coil `io.coil-kt.coil3:coil-compose` + `coil-network-okhttp` (or coil-compose 2.x if catalog simpler) — add to version catalog; needed to load https avatars
- Strings: change photo, remove photo, errors

Storage path: `{userId}/avatar.jpg`. After upload, set `avatar_url` to `supabase.storage.from("avatars").publicUrl`. `clearAvatar`: delete object + set column null.

- [ ] **Step 1: Repository tests with fake remote** — upload writes bytes and updates url; clear nulls url.

- [ ] **Step 2: Implement Storage** — log failures with `Log.e`; never log full user email.

- [ ] **Step 3: Preferences** — `rememberLauncherForActivityResult(PickVisualMedia())`. Read URI → JPEG bytes (compress, max ~512px) on IO dispatcher. Show Coil `AsyncImage` if url non-null else initials. Buttons/menus: Change photo, Remove photo. Online-first errors via events.

- [ ] **Step 4: Compile + unit tests.**

---

### Task 5: Avatars on Profile, Home, Friends

**Files:**
- `ProfileScreen.kt` header
- `HomeScreen.kt` greeting avatar
- `FriendsScreen.kt` `Avatar(profile)`
- Shared helper e.g. `presentation/components/ProfileAvatar.kt` (`url`, `displayName`, `avatarColor`, `size`)

- [ ] **Step 1: Extract `ProfileAvatar`** — Coil if url, else initials on `avatarColor` circle.

- [ ] **Step 2: Replace existing `Avatar` / header circles.** No behavior change when url is null.

- [ ] **Step 3: Compile.** Preview with null url.

---

### Task 6: Delete account — Edge Function + confirmation screen

**Files:**
- Create: `supabase/functions/delete-account/index.ts`
- Create: `DeleteAccountScreen.kt`, `DeleteAccountViewModel.kt`
- Modify: `ProfileRepository.deleteAccount`, `SocialRemoteDataSource` `functions.invoke("delete-account")`
- Nav: `TabDeleteAccount` from Preferences
- Strings: body copy, `DELETE` token (`preferences_delete_confirm_token` = `DELETE`, same in ES), errors
- Test: ViewModel — button disabled until input == `DELETE`; success navigates logged-out

Edge Function (sketch):

```ts
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
Deno.serve(async (req) => {
  const auth = req.headers.get("Authorization");
  if (!auth) return new Response("Unauthorized", { status: 401 });
  const userClient = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: auth } },
  });
  const { data: { user } } = await userClient.auth.getUser();
  if (!user) return new Response("Unauthorized", { status: 401 });
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  await admin.storage.from("avatars").remove([`${user.id}/avatar.jpg`]);
  const { error } = await admin.auth.admin.deleteUser(user.id);
  if (error) return new Response(error.message, { status: 500 });
  return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
});
```

Deploy with Supabase CLI / dashboard. JWT verify on.

- [ ] **Step 1: Function + deploy** (or document deploy if credentials missing; still commit source).

- [ ] **Step 2: Repository `deleteAccount`** — network check; invoke function; on success `signOut`.

- [ ] **Step 3: Confirmation UI** — explanation; `TextField`; Delete enabled iff trimmed input == `DELETE`; Cancel pops. Success event → `onSignedOut` (same as Profile sign-out).

- [ ] **Step 4: Preferences footer button navigates to delete screen. Tests + compile.**

---

### Task 7: Verification

- [ ] `compileDebugKotlin`
- [ ] Unit tests for mapper, UpdateDisplayName, PreferencesViewModel, DeleteAccountViewModel
- [ ] Manual: change name, pick photo, remove photo, friends see avatar, type DELETE, account gone, login screen

---

## Spec coverage

| Requirement | Task |
|-------------|------|
| `avatar_url` + Storage | 1, 4 |
| Edit display_name, profiles source of truth | 2, 3 |
| Photo Picker gallery | 4 |
| Avatars in Profile/Home/Friends | 5 |
| Delete + `DELETE` screen + Edge Function | 6 |

## Placeholder scan

No TBD. Task 2 must not leave `TODO()` in shipped Kotlin.
