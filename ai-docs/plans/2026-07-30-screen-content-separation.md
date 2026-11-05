# Screen/Content Separation Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Every feature screen uses a thin `*Screen` (ViewModel + events only) and a VM-free `*Content` for UI; document the rule in the Compose skill and AGENTS.md.

**Architecture:** Unidirectional data flow. Screen collects state / wires callbacks; Content is pure Compose for Preview and tests.

**Tech Stack:** Jetpack Compose, Hilt ViewModel, StateFlow

**Platform:** Android

**Approved by user:** "HAZ ESTO Y AÑADELO A LA SKILL DE COMPOSE" (2026-07-30)

---

### Task 1: Document the rule

**Files:**
- Modify: `C:\Users\marta\.agents\skills\android-jetpack-compose\SKILL.md`
- Modify: `AGENTS.md`, `spec.md` (View section)

**Steps:**
- [x] Add "Screen / Content separation" section to Compose skill (English, with example mirroring AddContent)
- [x] Add matching bullet under presentation rules in AGENTS.md and spec.md View

### Task 2: Auth + Onboarding screens

**Files:**
- Modify: `LoginScreen.kt`, `RegisterScreen.kt`, `OnboardingScreen.kt`

**Pattern (reference `AddContentScreen`):**
```kotlin
@Composable
fun FooScreen(vm: FooViewModel = hiltViewModel(), ...) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    // LaunchedEffect for one-shots only
    FooContent(uiState = uiState, onX = vm::onX, ...)
}

@Composable
fun FooContent(uiState: FooUiState, onX: (...) -> Unit, ...) { /* all UI */ }
```

- [x] Extract `LoginContent` (public, no ViewModel)
- [x] Extract `RegisterContent`
- [x] Extract `OnboardingContent` (pass slides/page callbacks or derived state — no VM inside Content)

### Task 3: Home, Stats, Profile, Detail, BottomNav

**Files:**
- Modify: `HomeScreen.kt`, `StatsScreen.kt`, `ProfileScreen.kt`, `HabitDetailScreen.kt`, `BottomNavScreen.kt`

- [x] Extract `*Content` for each; Screen only collects + wires
- [x] If multiple flows, pass concrete params / a ui model — Content must not call `hiltViewModel` or take ViewModel type

### Task 4: Make AddContentContent public + verify

**Files:**
- Modify: `AddContentScreen.kt` (Content currently `private` → public for Preview)

- [x] `AddContentContent` public
- [x] `./gradlew :app:compileDebugKotlin`
- [x] Stage all touched files (skill path is outside repo — note in completion; stage repo files)

## Out of scope

- Adding @Preview composables now (only enable the pattern)
- Navigation wrappers / AppStartViewModel shell
- Committing unless user asks
