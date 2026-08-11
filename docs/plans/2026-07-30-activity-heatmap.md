# Activity Heatmap Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans` / subagents as needed.

**Goal:** Add a shared Activity heatmap (4×7, last 4 weeks) to Habit Detail Calendar and Stats, with inverted colors (white = less, dark purple = more).

**Architecture:** Reusable `ActivityHeatmap` in `presentation/components`. Pure helpers build week grid + intensities. Detail uses binary intensity per habit; Stats uses completion % across scheduled habits that day.

**Platform:** Android

---

### Task 1: Spec + strings
- Update `spec.md` Activity heatmap section
- Add strings: activity_title, activity_less, activity_more, activity_week_n, day letters

### Task 2: Helpers + component
- Create `ActivityHeatmapHelpers.kt` (rolling 4 weeks Mon–Sun, intensity helpers)
- Create `ActivityHeatmap.kt` Compose UI
- Unit test for week grid / binary intensity

### Task 3: Wire screens
- Detail `CalendarTab` → heatmap from habit records
- Stats screen → global heatmap above habit list

### Task 4: Verify
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` (habit-related + new tests)
- Stage files
