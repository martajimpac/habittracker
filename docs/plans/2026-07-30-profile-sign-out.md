# Profile Sign Out Implementation Plan

> **Goal:** Wire Profile Sign Out to Supabase logout and navigate to Login. Other profile menu rows stay no-op.

**Platform:** Android

---

- [x] Update `spec.md` (Sign Out real)
- [x] Add `AuthRepository.signOut()` + Fake + Impl (`supabase.auth.signOut()`, Log on failure)
- [x] Failing test: ProfileViewModel emits navigate-to-login after successful signOut
- [x] ProfileViewModel `onSignOutClicked` + SharedFlow; ProfileContent callback
- [x] Thread `onSignedOut` through BottomNav → NavigationWrapper → Login (clear back stack)
- [x] Compile + tests + stage
