# Glance Home Widgets Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans`. Checkboxes track progress. Spec: `spec.md` § Home screen widgets (Jetpack Glance).

**Goal:** Ship three Jetpack Glance home-screen widgets — configurable habit (with online-first toggle), configurable challenge (read-only + snapshot), and weekly summary (read-only).

**Architecture:** Glance `GlanceAppWidget` + config Activities on pin. Habit/week UI state from Room via `HabitRepository`. Challenge widget stores `challengeId` + JSON snapshot in DataStore (keyed by `appWidgetId`) because challenges are remote-only. Habit toggle calls `HabitRepository.toggleHabitCompletion` (online-first). Hilt via `@AndroidEntryPoint` / `EntryPointAccessors` for Glance callbacks. Refresh: after toggle, on `MainActivity` resume, and periodic `GlanceAppWidgetManager` / WorkManager (~45 min).

**Tech Stack:** Jetpack Glance, App Widgets, DataStore Preferences, Hilt, Room (existing), WorkManager (optional for periodic)

**Platform:** Android

**Approved:** 2026-08-07 (spec + brain)

---

## File map

| Area | Paths |
|------|--------|
| Gradle | `gradle/libs.versions.toml`, `app/build.gradle.kts` — Glance (+ WorkManager if used) |
| Manifest / XML | `AndroidManifest.xml`, `res/xml/*_widget_info.xml`, `res/layout/*_widget_preview` optional |
| Widget prefs | `data/local/datastore/WidgetPreferencesDataSource.kt` (+ keys) |
| Domain helpers | `domain/usecase/BuildWeeklyHabitSummary.kt` (pure) |
| Widgets | `presentation/widgets/habit/`, `challenge/`, `weekly/` — Receiver, GlanceAppWidget, ConfigActivity, actions |
| App hook | `MainActivity.kt` — request widget updates on resume |
| Strings | `strings.xml` |
| Tests | Widget prefs + weekly summary + toggle action unit tests |

**Suggested package root:** `com.marta.habittracker.presentation.widgets`

---

### Task 1: Dependencies + catalog

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [x] Add Glance (align with Compose BOM or pin a stable `glance` version compatible with AGP/Kotlin in project). Prefer BOM if available; otherwise document version.
- [x] Add `androidx.glance:glance-appwidget` (+ `glance-material3` if used).
- [x] Optional: `work-runtime-ktx` for periodic refresh.
- [x] Sync Gradle / `./gradlew :app:compileDebugKotlin` (may fail until code exists — at least resolve deps).

```toml
# example — verify latest compatible; do not invent if catalog already has a pattern
glance = "1.1.1"
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
```

- [x] Stage

---

### Task 2: Widget preferences DataSource (TDD)

**Files:**
- Create: `data/local/datastore/WidgetPreferencesKeys.kt`
- Create: `data/local/datastore/WidgetPreferencesDataSource.kt`
- Test: `test/.../WidgetPreferencesDataSourceTest.kt` (Robolectric + includeAndroidResources already enabled)

API sketch:

```kotlin
class WidgetPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    suspend fun setHabitWidgetHabitId(appWidgetId: Int, habitId: String)
    suspend fun getHabitWidgetHabitId(appWidgetId: Int): String?
    suspend fun setChallengeWidget(appWidgetId: Int, challengeId: String, snapshotJson: String)
    suspend fun getChallengeWidget(appWidgetId: Int): ChallengeWidgetPrefs?
    suspend fun clearWidget(appWidgetId: Int)
}

data class ChallengeWidgetPrefs(val challengeId: String, val snapshotJson: String)
```

Keys: `"habit_widget_$id"`, `"challenge_id_$id"`, `"challenge_snap_$id"`.

- [ ] Failing test → implement → green (blocked: `WidgetPreferencesDataSourceTest` currently fails during WorkManager initialization)
- [x] Stage

---

### Task 3: Weekly summary domain helper (TDD)

**Files:**
- Create: `domain/usecase/BuildWeeklyHabitSummary.kt`
- Test: `test/.../BuildWeeklyHabitSummaryTest.kt`

```kotlin
data class DaySummary(val date: LocalDate, val completed: Int, val scheduled: Int) {
    val percent: Int get() = if (scheduled == 0) 0 else (completed * 100) / scheduled
}

/** Rolling Mon–Sun week containing [anchor], or last 7 days Mon-aligned — document choice (prefer Mon–Sun of current week like Stats). */
fun buildWeeklyHabitSummary(habits: List<Habit>, anchor: LocalDate): List<DaySummary>
```

Reuse existing “scheduled that day” logic from Stats/Home if extractable without pulling presentation deps.

- [x] Tests for empty habits, partial week, weekend
- [x] Stage

---

### Task 4: Habit Glance widget + config

**Files:**
- Create: `presentation/widgets/habit/HabitWidgetReceiver.kt`
- Create: `presentation/widgets/habit/HabitGlanceWidget.kt`
- Create: `presentation/widgets/habit/HabitWidgetConfigActivity.kt`
- Create: `presentation/widgets/habit/ToggleHabitAction.kt` (Glance action callback)
- Create: `res/xml/habit_widget_info.xml`
- Modify: `AndroidManifest.xml` — receiver + config activity
- Modify: `strings.xml`

**Behavior:**
1. Config Activity (`@AndroidEntryPoint`): list habits from `HabitRepository.getAllHabitsWithRecords()`, save habitId to DataStore for `appWidgetId`, `RESULT_OK`, update widget.
2. Glance UI: load habit by id from Room; show name, today completed state, toggle button.
3. `ToggleHabitAction`: `EntryPoint` → `HabitRepository.toggleHabitCompletion(habit, LocalDate.now())`; on Network error log + leave UI unchanged; on success `HabitGlanceWidget().update(...)`.
4. Click body → deep link / `MainActivity` (optional habitId extra).

Sizes: `minWidth`/`minHeight` suitable for 2×2.

- [x] Compile
- [x] Stage

---

### Task 5: Challenge Glance widget + config (read-only)

**Files:**
- Create: `presentation/widgets/challenge/ChallengeWidgetReceiver.kt`
- Create: `presentation/widgets/challenge/ChallengeGlanceWidget.kt`
- Create: `presentation/widgets/challenge/ChallengeWidgetConfigActivity.kt`
- Create: `res/xml/challenge_widget_info.xml`
- Modify: manifest + strings

**Behavior:**
1. Config: if offline, show error string; if online, `FriendsRepository.getActiveChallenges()`, picker.
2. On select: persist `challengeId` + snapshot JSON (`ChallengeCard` fields: opponent, progress, daysLeft, habitName/color).
3. Widget paints from snapshot; background refresh coroutine tries `getActiveChallenges` and rewrites snapshot when online.
4. No toggle. Tap → `MainActivity` (Friends tab if easy; else launcher).

- [x] Compile
- [x] Stage

---

### Task 6: Weekly Glance widget

**Files:**
- Create: `presentation/widgets/weekly/WeeklyWidgetReceiver.kt`
- Create: `presentation/widgets/weekly/WeeklyGlanceWidget.kt`
- Create: `res/xml/weekly_widget_info.xml`
- Modify: manifest + strings

**Behavior:**
- No config activity (`configure` omitted).
- Read all habits with records → `buildWeeklyHabitSummary` → 7 day cells (initials Mon–Sun + %).
- Tap → MainActivity / Stats.

- [x] Compile
- [x] Stage

---

### Task 7: Refresh hooks

**Files:**
- Modify: `MainActivity.kt` (or `HabitTracker` Application) — on start/resume update all three widget types via `GlanceAppWidgetManager` / `updateAll`.
- Create: `presentation/widgets/WidgetRefresher.kt` helper
- Optional: `WidgetRefreshWorker` (WorkManager Periodic 45 min) + enqueue in `HabitTracker.onCreate`

Also call refresher after successful toggle in `ToggleHabitAction`.

- [x] Compile
- [x] Stage

---

### Task 8: Verification + spec polish

- [ ] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` (2026-08-07: `:app:compileDebugKotlin` passed; 77/78 unit tests passed. `RegisterUseCaseTest.invoke returns InvalidEmail when email is blank` fails during Robolectric font loading with `FileSystemAlreadyExistsException`; widget preferences tests no longer fail during WorkManager initialization.)
- [ ] Manual checklist (device):
  - [ ] Add habit widget → pick habit → toggle with network → Room/UI update
  - [ ] Toggle offline → no change
  - [ ] Add challenge widget → pick challenge → shows snapshot offline
  - [ ] Weekly widget shows 7 days
- [x] Confirm `spec.md` widgets section still accurate; mark plan checkboxes
- [x] Stage all widget-related files

---

## Dependency graph

```text
Task1 deps ─┬─► Task2 prefs ─┬─► Task4 habit widget ─┐
            │                ├─► Task5 challenge ────┼─► Task7 refresh ─► Task8
            └─► Task3 weekly math ─► Task6 weekly ───┘
```

**Parallel-safe after Task1:** Task2 ‖ Task3; then Task4 ‖ Task5 ‖ Task6 (different packages); Task7 last.

---

## Out of scope

Completing from challenge/weekly widgets; Profile-based widget config; offline mutation queue; multipage mega-widget; fancy Glance animations.

---

## Spec coverage

| Spec | Task |
|------|------|
| Habit widget + toggle online-first | 4, 7 |
| Challenge widget + snapshot | 2, 5 |
| Weekly summary read-only | 3, 6 |
| Config on add | 4, 5 |
| Strings / theme | 4–6 |
| Refresh cadence | 7 |

---

## Execution handoff

Plan saved to `docs/plans/2026-08-07-glance-home-widgets.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task
2. **Inline Execution** — this session with checkpoints

Which approach?
