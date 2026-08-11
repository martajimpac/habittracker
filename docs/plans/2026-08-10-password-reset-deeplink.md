# Password Reset (Deep Link) Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-tdd` then implement. Checkboxes track progress. Spec: `spec.md` § Password reset (deep link).

**Goal:** Forgot-password via Supabase email + custom-scheme deep link into an in-app Reset Password screen; on success sign out recovery session and return to Login.

**Architecture:** Clean Architecture — `AuthRepository` + use cases; Forgot/Reset Screen+ViewModel; Navigation deep link; `supabase.handleDeeplinks(intent)` in `MainActivity`; one-shot `SharedFlow` + `CollectAsEffect`.

**Tech:** Supabase Auth Kotlin (`resetPasswordForEmail`, `updateUser`, `handleDeeplinks`), custom scheme `habittracker://auth/reset`, Hilt, Compose Navigation type-safe.

**Platform:** Android

**Approved design:** 2026-08-08 / confirmed 2026-08-10

---

## File map

| Area | Paths |
|------|--------|
| Auth config | `di/SupabaseModule.kt` — `install(Auth) { scheme = "habittracker"; host = "auth" }` (path `/reset` via deep link) |
| Manifest | `AndroidManifest.xml` — VIEW intent-filter on `MainActivity` (`habittracker` / `auth`) |
| Domain | `AuthRepository` + `RequestPasswordReset` / `UpdatePassword` (or repo methods + thin use cases); errors if needed |
| Data | `AuthRepositoryImpl` — reset email, update password, expose/handle deeplink session via client |
| Deeplink bridge | `MainActivity` + small `AuthDeeplinkHandler` / inject SupabaseClient to call `handleDeeplinks` |
| Nav | `Screens.kt` — `ForgotPassword(email?)`, `ResetPassword`; wire in `NavigationWrapper`; deep link to Reset |
| UI | `presentation/screens/auth/forgot_password/*`, `.../reset_password/*` |
| Login | Wire `onForgotPassword` → navigate Forgot (pass current email) |
| Strings | `strings.xml` EN |
| Tests | Use case / VM tests for validation, success emit, no sticky flags; FakeAuthRepository methods |

**Manual (human):** Supabase Dashboard → Auth → Redirect URLs → add `habittracker://auth/reset`

---

### Task 1: Domain + FakeAuthRepository (TDD)

**Files:**
- Modify: `domain/repository/AuthRepository.kt`
- Modify: `data/repository/AuthRepositoryImpl.kt` (stub later if needed)
- Modify: `app/src/test/.../FakeAuthRepository.kt`
- Add: use cases `RequestPasswordReset.kt`, `UpdatePassword.kt` (+ tests)

- [x] Add `requestPasswordReset(email: String): DataResult<Unit, AppError>`
- [x] Add `updatePassword(newPassword: String): DataResult<Unit, AppError>` (assumes recovery session already imported)
- [x] Optionally `signOut` already exists — call after successful update from use case or VM
- [x] `RequestPasswordReset`: validate email with `EmailValidator`; then repo
- [x] `UpdatePassword`: validate with `PasswordValidator`; then repo; then `signOut`
- [x] RED tests → GREEN

---

### Task 2: AuthRepositoryImpl + Supabase Auth deeplink config

**Files:**
- Modify: `AuthRepositoryImpl.kt`, `SupabaseModule.kt`

- [x] `resetPasswordForEmail(email, redirectUrl = "habittracker://auth/reset")`
- [x] `updateUser { password = ... }`
- [x] Map network/auth errors; Log.d/e with TAG; never log tokens/passwords
- [x] `install(Auth) { scheme = "habittracker"; host = "auth" }` so platform deeplink matches
- [x] Compile

---

### Task 3: Manifest + MainActivity handleDeeplinks

**Files:**
- Modify: `AndroidManifest.xml`, `MainActivity.kt`
- Optionally: inject `SupabaseClient` into MainActivity or an `@AndroidEntryPoint` helper

- [x] Intent-filter VIEW/BROWSABLE: `scheme=habittracker`, `host=auth`, `pathPrefix=/reset` (or pathPattern)
- [x] `onCreate` + `onNewIntent`: `supabase.handleDeeplinks(intent)` then if recovery session → navigate to Reset
- [x] Prefer signaling nav via a small shared flow / pending deeplink flag in a Hilt singleton `AuthDeeplinkCoordinator` consumed by `NavigationWrapper` (avoid sticky UiState)

---

### Task 4: ForgotPassword UI + nav from Login

**Files:**
- Add: `ForgotPasswordScreen.kt`, `ForgotPasswordViewModel.kt`
- Modify: `Screens.kt`, `NavigationWrapper.kt`, `LoginScreen.kt`

- [x] Screen/Content split; email field; send button; loading/error
- [x] On success: generic message + `navigateBackToLogin` SharedFlow
- [x] Login: `CollectAsEffect` / navigate to Forgot with email arg
- [x] Strings EN
- [x] VM unit tests (TDD-ish; added before implementation)

---

### Task 5: ResetPassword UI + deep link entry

**Files:**
- Add: `ResetPasswordScreen.kt`, `ResetPasswordViewModel.kt`
- Modify: navigation + deeplink coordinator

- [x] New password + confirm; enable when valid + match
- [x] On success: UpdatePassword use case → navigate Login SharedFlow + success snackbar/message arg
- [x] Deep link opens Reset only after `handleDeeplinks` imported session; invalid/expired link → Login + error string
- [x] VM unit tests (TDD-ish; added before implementation)

---

### Task 6: Verify

- [x] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` (fresh `--rerun-tasks`: 108 tests, 0 failures)
- [ ] Manual checklist: Redirect URL in Supabase; send email; open link; set password; land on Login
- [x] Stage task files; do not commit unless asked

---

## Dependency graph

```
Task1 → Task2 → Task3 → Task4 ∥ Task5 → Task6
```

Tasks 4 and 5 can parallelize after Task 3 (deeplink coordinator + nav routes exist).

## Out of scope

Web page, App Links, Profile change-password, iOS.
