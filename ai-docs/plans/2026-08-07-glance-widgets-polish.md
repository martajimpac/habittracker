# Glance Widgets Polish Plan

> Spec: `spec.md` § Home screen widgets. Closes known gaps from Task 8 verification.

**Goal:** Align Habit / Challenge / Weekly Glance widgets with the approved spec (visuals, picker eligibility, deep links, cleanup).

## Gaps → tasks

| # | Gap | Work | Done |
|---|-----|------|------|
| 1 | Habit missing icon/color (+ streak if fits) | HabitGlanceWidget + shared color/icon helpers; string for streak | [x] |
| 2 | Challenge empty copy says “active” only | Strings + show status in config list (API already returns pending+active) | [x] |
| 3 | Challenge tap → Friends | Intent extra → MainActivity → BottomNav initial tab | [x] |
| 4 | Weekly tap → Stats/Home | Same deep-link path → Stats tab | [x] |
| 5 | Habit body tap → Home (or detail) | Deep link Home; optional habitId for future detail | [x] |
| 6 | Theme colors on widgets | Glance background / accent from HabitPrimary + habit colorHex | [x] |
| 7 | Clear prefs on delete | Receivers `onDeleted` → WidgetPreferencesDataSource.clearWidget | [x] |
| 8 | Config pickers show icon/color | Habit + Challenge config ListItems | [x] |
| 9 | Tests | Deep-link parsing unit tests; streak helper; snapshot status if added | [x] |

## Approach

- Small `WidgetIntents` / `WidgetLaunchExtras` in `presentation/widgets` with constants + Intent builders.
- `MainActivity` reads extras; pass `initialTab` into `NavigationWrapper` → `BottomNavScreen`.
- Move `calculateStreak` usage for widgets via a tiny domain/presentation helper already on Home (`calculateStreak`) — call from widget state builder (presentation OK).
- Habit icon in Glance: use `androidx.glance.Image` + `ImageProvider(habitIconRes(...))` (same keys as Compose).
- Color: `ColorProvider` / `GlanceModifier.background` with parsed hex (fallback HabitPrimary).

## Out of scope

- Offline mutations, Profile config, multipage widget, instrumented UI on device.
