# Align codebase to AGENTS.md data structure

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Restructure packages so the HabitTracker source tree matches the updated `AGENTS.md` layout and DataStore/DataSource rules, and the project compiles with green unit tests.

**Architecture:** Keep Clean Architecture (`presentation → domain ← data`). Move data files into `local/room`, `local/datastore`, `remote`, `repository`, and `mapper`. Encapsulate DataStore behind a DataSource (repositories must not touch DataStore). Shared FCM service moves to `core`. Remove leftover `view/` duplicates and restore presentation AppStart/Onboarding ViewModels against `OnboardingRepository`.

**Tech Stack:** Kotlin, Hilt, Room, DataStore, Supabase, Jetpack Compose

**Platform:** Android

---

## Target tree (data)

```text
data/
├── local/
│   ├── room/          # Dao, Database, Converters, entities/
│   └── datastore/     # keys + OnboardingPreferencesDataSource
├── remote/            # api/, response/, (no repository impls)
├── repository/        # HabitRepositoryImpl, AuthRepositoryImpl, OnboardingRepositoryImpl
└── mapper/            # HabitMapper
```

```text
core/
├── DateTimeExtensions.kt
├── DefaultDispatchersProvider.kt
├── SystemClock.kt                 # domain Clock impl (shared infra)
└── MyFirebaseMessagingService.kt  # shared service
```

---

### Task 1: Move Room stack to `data/local/room`

**Files:**
- Move: `data/local/database/**` → `data/local/room/**` (package `…data.local.room` / `…entities`)
- Modify: `di/DatabaseModule.kt` imports
- Modify: `androidTest/.../HabitDaoSecurityInstrumentedTest.kt` imports

- [ ] **Step 1:** `git mv` database files to `local/room` (keep `entities/` subpackage)
- [ ] **Step 2:** Update all `package` / `import` from `data.local.database` → `data.local.room`
- [ ] **Step 3:** Update DI + instrumented test imports

---

### Task 2: Move mapper to `data/mapper`

**Files:**
- Move: `HabitMapper.kt` → `data/mapper/HabitMapper.kt` (package `…data.mapper`)
- Modify: `HabitRepositoryImpl` import (after Task 3)

- [ ] **Step 1:** Move + update package
- [ ] **Step 2:** Fix all HabitMapper imports

---

### Task 3: Move repository impls to `data/repository`

**Files:**
- Move: `HabitRepositoryImpl` → `data/repository/`
- Move: `AuthRepositoryImpl` → `data/repository/`
- Modify: `di/DataModule.kt` binds/imports

- [ ] **Step 1:** Move Habit + Auth impls; update packages to `…data.repository`
- [ ] **Step 2:** Update DataModule binds

---

### Task 4: DataStore DataSource + move prefs to `data/local/datastore`

**Files:**
- Create: `data/local/datastore/OnboardingPreferencesDataSource.kt`
- Move: `OnboardingPreferencesKeys.kt` → `data/local/datastore/`
- Create/Move: `OnboardingRepositoryImpl` → `data/repository/` using **only** the DataSource (no `DataStore` in repo)
- Modify: `di/DataModule.kt` (bind OnboardingRepository; provide/bind DataSource)
- Modify: `di/PreferencesModule.kt` if needed (DataStore provider stays; DataSource injects DataStore)

Per AGENTS: *ViewModel, use case, and repository must not access DataStore directly.*

- [ ] **Step 1:** Create DataSource wrapping read/write of onboarding flag
- [ ] **Step 2:** Implement `OnboardingRepositoryImpl(dataSource)` in `data/repository`
- [ ] **Step 3:** Bind DataSource + OnboardingRepository in root `di/`
- [ ] **Step 4:** Delete old `data/local/preferences/` package

---

### Task 5: Move remote + core shared types

**Files:**
- Move: `data/network/**` → `data/remote/**` (packages `…data.remote`, `…api`, `…response`)
- Move: `data/service/MyFirebaseMessagingService.kt` → `core/`
- Move: `data/util/SystemClock.kt` → `core/`
- Modify: `AndroidManifest.xml` FCM service name
- Modify: any Clock / FCM imports

- [ ] **Step 1:** Move network → remote; update packages/imports
- [ ] **Step 2:** Move FCM service + SystemClock to core; update manifest + DI/imports
- [ ] **Step 3:** Delete empty `data/network`, `data/service`, `data/util`

---

### Task 6: Remove `view/` leftovers; restore presentation start VMs

**Files:**
- Delete: entire `view/` tree under main
- Create: `presentation/navigation/AppStartViewModel.kt` using `OnboardingRepository` + `AuthRepository`
- Ensure: `presentation/screens/onboarding/OnboardingViewModel.kt` uses `OnboardingRepository` only
- Modify: `NavigationWrapper` already references presentation `AppStartViewModel`

- [ ] **Step 1:** Write presentation AppStartViewModel (domain repos only)
- [ ] **Step 2:** Fix/create presentation OnboardingViewModel
- [ ] **Step 3:** Delete `view/` sources so Hilt has no duplicate ViewModels

---

### Task 7: Verify + stage

- [ ] **Step 1:** `./gradlew.bat compileDebugKotlin testDebugUnitTest`
- [ ] **Step 2:** Stage all task files including `docs/plans/`, `AGENTS.md`, manifest, sources, tests (per git-staging rule)
- [ ] **Step 3:** Confirm no remaining imports of `data.local.database`, `data.network`, `data.local.preferences`, `data.service`, or `view.`

---

## Dependency graph

```text
Task 1 (room) → Task 2 (mapper) → Task 3 (repos)
Task 4 (datastore/datasource) parallel with 1–3 after keys exist
Task 5 (remote/core) parallel after AuthRepositoryImpl moved (Task 3)
Task 6 after Task 4 (needs OnboardingRepository wired)
Task 7 last
```

## Out of scope

- Adding `Contract.kt` per screen
- Changing Room schema / migrations
- Rewriting Supabase/auth behavior
