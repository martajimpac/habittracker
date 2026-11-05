# Channel + ObserveAsEvents Migration Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` or `mobiai-mobile-executing-plans`. Checkboxes track progress.

**Goal:** Replace all one-shot `SharedFlow` + `CollectAsEffect` with `Channel(BUFFERED)` + `receiveAsFlow()` + `ObserveAsEvents`.

**Architecture:** Persistent UI stays `StateFlow`. One-shot effects use Channel. Shared helper in `presentation/utils`. Keep existing flow property names (`navigateToHome`, etc.) where a single `Unit` event exists; AuthDeeplink keeps sealed `AuthDeeplinkEvent`.

**Tech Stack:** Kotlin Coroutines Channel, Compose, lifecycle-runtime-compose

**Platform:** Android

**Approved design:** 2026-08-17 (full migrate)

---

## File map

| Action | Path |
|--------|------|
| Create | `presentation/utils/ObserveAsEvents.kt` |
| Delete | `presentation/utils/CollectAsEffect.kt` |
| Modify VMs | Login, Register, ForgotPassword, ResetPassword, Onboarding, Profile, HabitDetail, AddContent |
| Modify | `AuthDeeplinkCoordinator.kt`, `AuthDeeplinkViewModel.kt` |
| Modify Screens | Login, Register, ForgotPassword, ResetPassword, Onboarding, Profile, HabitDetail, AddContent, NavigationWrapper |
| Modify tests | LoginViewModelTest, AddContentViewModelTest (+ any that break) |

---

### Task 1: ObserveAsEvents helper; remove CollectAsEffect

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/presentation/utils/ObserveAsEvents.kt`
- Delete: `app/src/main/java/com/marta/habittracker/presentation/utils/CollectAsEffect.kt`

- [x] Add public helper:

```kotlin
package com.marta.habittracker.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
```

- [x] Delete `CollectAsEffect.kt` (call sites still compile-broken until Task 3 — ok if Tasks 2–3 follow immediately in same session)
- [x] Stage

---

### Task 2: Migrate ViewModels + AuthDeeplinkCoordinator to Channel

For each single-event VM, replace:

```kotlin
private val _x = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
val x: SharedFlow<Unit> = _x.asSharedFlow()
// emit:
_x.emit(Unit) // or tryEmit
```

with:

```kotlin
private val _x = Channel<Unit>(Channel.BUFFERED)
val x = _x.receiveAsFlow()
// send:
_x.send(Unit)
```

**Files:**
- `LoginViewModel.kt` — `navigateToHome`
- `RegisterViewModel.kt` — `navigateToHome`
- `ForgotPasswordViewModel.kt` — `navigateToLogin`
- `ResetPasswordViewModel.kt` — `navigateToLogin`
- `OnboardingViewModel.kt` — `navigateToLogin`
- `ProfileViewModel.kt` — `navigateToLogin`
- `HabitDetailViewModel.kt` — `habitDeleted`
- `AddContentViewModel.kt` — `habitSaved`
- `AuthDeeplinkCoordinator.kt` — `Channel<AuthDeeplinkEvent>`; `tryEmit` → `trySend` / `send`
- `AuthDeeplinkViewModel.kt` — expose `Flow` (not `SharedFlow`)

Imports: `kotlinx.coroutines.channels.Channel`, `receiveAsFlow`, remove SharedFlow imports.

- [x] Implement all replacements
- [x] `.\gradlew.bat :app:compileDebugKotlin` may still fail on Screens until Task 3
- [x] Stage VM + coordinator files

---

### Task 3: Migrate Screens + NavigationWrapper

Replace every:

```kotlin
CollectAsEffect(viewModel.xyz) { ... }
```

with:

```kotlin
ObserveAsEvents(flow = viewModel.xyz) { ... }
```

(For AuthDeeplink: `ObserveAsEvents(flow = authDeeplinkViewModel.events) { ... }`)

Update imports. Ensure no remaining `CollectAsEffect` references.

- [x] All Screens + NavigationWrapper
- [x] `rg CollectAsEffect app/src` → zero
- [x] `.\gradlew.bat :app:compileDebugKotlin` → SUCCESS
- [x] Stage

---

### Task 4: Tests + verify

- [x] Fix unit tests that assume SharedFlow API if needed (`LoginViewModelTest`, `AddContentViewModelTest` — `first()` / `collect` on Flow still work)
- [x] `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` → 0 failures
- [x] `rg "MutableSharedFlow|CollectAsEffect" app/src` → zero (except comments/docs if any)
- [x] Mark plan checkboxes; stage all task files; do not commit unless asked

---

## Dependency graph

```
Task1 → Task2 → Task3 → Task4
```

## Out of scope

New dialogs/snackbars; renaming every event to a single `events` sealed type per screen when a named `Unit` flow already exists.
