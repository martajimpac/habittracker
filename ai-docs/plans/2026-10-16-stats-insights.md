# Stats insights Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** The Stats tab names the strongest weekday and the habit whose current streak ends if tomorrow is missed.

**Architecture:** `habitStatsInsight` reads the habits already loaded from Room. Completed records are grouped by weekday; a tie keeps Monday-first. The streak uses `calculateHabitStreak`, the same consecutive-days-ending-today rule as Home. The longest alive streak is the one shown.

**Tech Stack:** Kotlin, Jetpack Compose, existing habit records.

**Platform:** Android

---

### Task 1: Insight calculation

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/usecase/HabitStatsInsight.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/usecase/HabitStatsInsightTest.kt`

- [x] Strongest weekday is the most completed records. Monday wins a tie. Incomplete records do not count.
- [x] A habit not completed today is not at risk tomorrow.
- [x] Among alive streaks, the longest one is shown.

### Task 2: Stats copy

**Files:**
- Modify: `StatsScreen.kt`, `values/strings.xml`, `values-es/strings.xml`, `spec.md`

- [x] Two sentences above the heatmap, from string resources.
- [x] Empty insights have their own sentences.
