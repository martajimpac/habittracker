# Profile Real Stats Implementation Plan

> **For agentic workers:** Screen/Content + ViewModel; reuse `calculateStreak` and habit records.

**Goal:** Profile header Day Streak / Completed / Habits come from real habit data (not mock strings).

**Definitions (align with Home):**
- **Day Streak:** max `calculateStreak(records)` across all habits
- **Completed:** count of records with `isCompleted == true`
- **Habits:** number of habits

**Platform:** Android

---

- [x] Update `spec.md` Profile section (remove mock; document real metrics)
- [x] Extend `FakeHabitRepository` to return configurable habits for `getAllHabitsWithRecords`
- [x] Failing test: ProfileViewModel exposes streak/completed/habits from repository
- [x] Implement ProfileViewModel + ProfileUiState / Content wiring
- [x] Remove unused mock string resources
- [x] Compile + unit tests + stage
