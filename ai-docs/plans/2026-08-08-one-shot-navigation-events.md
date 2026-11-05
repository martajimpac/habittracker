# One-shot navigation events (SharedFlow)

> Spec / architecture: navigation must not be driven by sticky UI state flags.

**Goal:** Eliminate “ghost navigation” from sticky booleans (e.g. `LoginUiState.isUserLogged`) and collect one-shot nav events in a lifecycle-aware way across the app.

## Audit

| Screen / VM | Current | Action |
|-------------|---------|--------|
| **Login** | `isUserLogged` + `LaunchedEffect(uiState.isUserLogged)` | **Fix** → `SharedFlow` + remove flag |
| Register | Already `navigateToHome` SharedFlow | Harden collect with `repeatOnLifecycle` |
| Onboarding | Already `navigateToLogin` SharedFlow | Harden collect |
| Profile | Already `navigateToLogin` SharedFlow | Harden collect |
| AddContent | Already `habitSaved` SharedFlow | Harden collect + `extraBufferCapacity = 1` if missing |
| HabitDetail | `habitDeleted` SharedFlow | Wire/harden Screen collect if missing |
| BottomNav `initialTabRoute` | Intent deep-link, not sticky VM flag | Leave (different concern) |

## Pattern (canonical)

**ViewModel**
```kotlin
private val _navigateToHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()
// on success: _navigateToHome.emit(Unit)  // or tryEmit
```

**Screen** (lifecycle-aware)
```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.navigateToHome.collect { navigateToHome() }
    }
}
```

Optional small helper: `presentation/utils/CollectAsEffect.kt` → `fun <T> SharedFlow<T>.CollectAsEffect(block)` to avoid copy-paste.

## Tasks

1. [x] **TDD Login** — failing test: success login emits `navigateToHome` once; `LoginUiState` has no `isUserLogged`.
2. [x] **Login VM + Screen** — SharedFlow emit on success; remove sticky flag; lifecycle collect.
3. [x] **Harden collectors** — Register, Onboarding, Profile, AddContent, HabitDetail → `CollectAsEffect` / `repeatOnLifecycle(STARTED)`.
4. [x] **AddContent** — ensure `extraBufferCapacity = 1` on `habitSaved`.
5. [x] **HabitDetail Screen** — collect `habitDeleted` → `onBack`.
6. [x] **Compose skill + AGENTS.md** — document “no sticky-state navigation”.
7. [x] **Verify** — `compileDebugKotlin` + `testDebugUnitTest` green; staged (no commit).

## Dependency graph

```
Task1 (Login tests) → Task2 (Login impl) → Task3–5 (parallel harden) → Task6 → Task7
```

## Out of scope

- Changing Navigation Compose graph structure
- Widget deep-link `initialTabRoute` LaunchedEffect
- Channel vs SharedFlow debate (user specified SharedFlow)
