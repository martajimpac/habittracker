# Friends Empty State (UI-only) Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace Friends placeholder with Figma-aligned empty state: header title + compact “Add” button (no-op) and centered empty copy.

**Architecture:** Presentation-only. `FriendsScreen` / `FriendsContent` with `onAddFriend: () -> Unit = {}`. No ViewModel, domain, or remote.

**Tech Stack:** Jetpack Compose, Material 3 icons, existing Habit theme colors, `strings.xml`.

**Platform:** Android

**Approved design:** Approach A (2026-07-30) — Figma Make header chrome + empty body; no sheets/list.

---

### Task 1: Spec + strings (TDD strings via Robolectric)

**Files:**
- Modify: `spec.md` (Friends section)
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`)
- Create: `app/src/test/java/com/marta/habittracker/presentation/screens/friends/FriendsStringsTest.kt`

- [x] Write Robolectric test for new strings
- [x] Observed RED (unresolved `R.string.friends_*`)
- [x] Added strings + includeAndroidResources
- [x] GREEN

### Task 2: Friends UI

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/friends/FriendsScreen.kt`

- [x] Header + compact Add + empty state
- [x] Compile + unit test pass
- [x] Stage

## Out of scope

Lista de amigos, retos, bottom sheets, ViewModel, Supabase, navegación desde Add.
