# Today widget Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** A Glance home-screen widget lists today's scheduled habits and opens the detail of the first one that is not completed.

**Architecture:** `todayWidgetSnapshot` filters habits by weekday and keeps repository order. The first incomplete item is the launch id. `TodayGlanceWidget` reads Room through the existing widget entry point and starts `MainActivity` with the habit extra already used by the habit widget. Refresh goes through `WidgetRefresher`.

**Tech Stack:** Jetpack Glance, Room, existing `WidgetLaunchExtras`.

**Platform:** Android

---

### Task 1: Snapshot

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/presentation/widgets/today/TodayWidgetSnapshot.kt`
- Test: `app/src/test/java/com/marta/habittracker/presentation/widgets/today/TodayWidgetSnapshotTest.kt`

- [x] Only habits scheduled today appear, in input order.
- [x] The first incomplete habit is the one the tap opens.
- [x] All done, or none scheduled, opens no habit id.
- [x] The widget shows at most 5 rows and counts the rest.

### Task 2: Widget

**Files:**
- Create: `TodayGlanceWidget.kt`, `TodayWidgetReceiver.kt`, `today_widget_info.xml`, `today_widget_preview.xml`, debug preview
- Modify: `AndroidManifest.xml`, `WidgetRefresher.kt`, `strings.xml`, `values-es/strings.xml`, `spec.md`

- [x] Register the receiver and refresh it with the other widgets.
- [x] Copy comes from string resources in EN and ES.
