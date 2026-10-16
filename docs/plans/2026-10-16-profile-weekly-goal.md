# Profile weekly goal Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** The Profile Goals row opens a screen showing a fixed 5-day weekly goal computed from existing Room habit records.

**Architecture:** `weeklyGoalProgress` reuses `buildWeeklyHabitSummary` for the Monday–Sunday week that contains today. A day counts only when it has scheduled habits and every one of them is completed. `GoalsViewModel` reads `HabitRepository.getAllHabitsWithRecords()`. No new table and no editable target.

**Tech Stack:** Kotlin, Jetpack Compose, Room via the existing habit repository, Hilt.

**Platform:** Android

---

### Task 1: Count successful days

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/usecase/WeeklyGoal.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/usecase/WeeklyGoalTest.kt`

- [x] Target is 5. Empty week is 0.
- [x] A day counts only when every scheduled habit is completed.
- [x] A partial day and a day with no scheduled habits do not count.

### Task 2: Goals screen

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/presentation/screens/goals/GoalsViewModel.kt`
- Create: `app/src/main/java/com/marta/habittracker/presentation/screens/goals/GoalsScreen.kt`
- Test: `app/src/test/java/com/marta/habittracker/presentation/screens/goals/GoalsViewModelTest.kt`
- Modify: `TabScreens.kt`, `NavigationBottomWrapper.kt`, `ProfileScreen.kt`, `BottomNavScreen.kt`, `strings.xml`, `values-es/strings.xml`, `spec.md`

- [x] Profile Goals navigates to the screen and the bottom bar hides.
- [x] Copy is in EN and ES string resources.
- [x] Progress shows completed days over 5 for the current week.
