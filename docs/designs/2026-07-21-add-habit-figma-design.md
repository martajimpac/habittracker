# Add Habit Screen (Figma) Design

**Date:** 2026-07-21  
**Source:** Figma Make [Habit Tracker App Design](https://www.figma.com/make/HBHNolFUUZYOYFKfI150Kt/Habit-Tracker-App-Design) — `AddHabitScreen`

## Goal

Adapt the create-habit screen to match Figma, persist the new visual/reminder fields locally, and reuse those fields in Home, Stats, and Detail. Keep the app offline-first: Room remains the source for UI; Supabase sync comes later.

## Decisions

| Topic | Choice |
|-------|--------|
| Frequency UI | Day-by-day chips (not Figma presets Daily/Weekdays/Weekends/3x/week) |
| Reminder time | UI + Room persistence only; no local notifications yet |
| Description | Keep as optional field under name |
| Icon/color consumers | Create + Home + Stats + Detail (replace id-derived placeholders) |
| Schema approach | Extend `habits` entity (not a separate appearance table) |
| Room version | `20260127` |
| Future sync | Offline-first local cache; fields must be portable for later Supabase mapping |
| Remote identity | Add `remoteId: String?` now (null until sync) |

## Architecture

```text
AddContentScreen → AddContentViewModel → SaveHabit → HabitRepository → Room
Home / Stats / Detail ← Habit (domain) with icon, colorHex, reminderTime
```

- Domain models remain the contract between layers.
- Room entities and future Supabase DTOs stay in `data` and map to domain.
- No Supabase habit sync in this change — only schema readiness (`remoteId` + portable field types).

## Data model (`habits`, Room version `20260127`)

Existing fields kept: `id`, `name`, `description?`, `daysOfWeek`, `createdAt`.

New fields:

| Field | Type | Notes |
|-------|------|--------|
| `icon` | `String` | Emoji, default `"💧"` |
| `colorHex` | `String` | e.g. `"#6750A4"`, default primary |
| `reminderTime` | `String?` | `"HH:mm"` or null |
| `remoteId` | `String?` | Supabase id when synced; null for local-only |

Migration from previous schema: `ALTER TABLE` add columns with defaults; existing rows get default icon/color and `remoteId = null`.

### Sync readiness (future)

- Store color as hex string, icon as emoji string, time as `HH:mm` — JSON/SQL friendly.
- `remoteId` reserved for Supabase primary key / UUID.
- Local `id: Long` remains Room PK for offline relations (`habit_records`).
- Sync implementation (push/pull, conflict rules) is out of scope.

## UI — Create Habit

1. **Header:** purple gradient (`HabitPrimary` → `HabitPrimaryLight`), close (X), title, Save.
2. **Preview:** large icon tile tinted with selected color.
3. **Habit Name** (required).
4. **Description** (optional).
5. **Choose Icon:** 6-column grid, 12 Figma emojis.
6. **Choose Color:** 8 Figma palette circles; check on selected.
7. **Row:** Reminder Time picker | day-of-week chips.
8. **Create Habit** full-width CTA; enabled when name non-blank and ≥1 day selected; gradient uses selected color.

Copy via `strings.xml` (match existing app language conventions). State via `AddContentUiState` / ViewModel (UDF).

### Icon / color palettes (from Figma)

```text
Icons: 💧 🏃 📚 🧘 🍎 😴 ☕ 🎵 ✍️ ❤️ 🎯 ⚡
Colors: #6750A4 #0D9488 #D97706 #E11D48 #059669 #2563EB #EA580C #7C3AED
```

## Validation & errors

- Name required (trimmed).
- At least one day selected.
- `reminderTime` optional; if set, must be `HH:mm`.
- Icon/color always from the fixed palettes (defaults if missing).
- Persist failures → generic UI error; no internal details leaked.

## Home / Stats / Detail

- Use `habit.icon` and parse `habit.colorHex` instead of `habitEmoji(id)` / `habitAccentColor(id)`.
- Show `reminderTime` where time is displayed; fallback when null (e.g. “All day” / existing string).

## Out of scope

- Local notification scheduling / FCM for reminders.
- Frequency presets from Figma.
- Category as its own persisted field (Figma derives label from icon; not stored).
- Supabase habit CRUD / sync pipeline.

## Testing

- Unit: ViewModel validation; `SaveHabit` passes new fields.
- Room: migration to `20260127`; defaults on existing rows; insert/read new columns.
- No screenshot / UI automator suite required for this change.

## Success criteria

- Create Habit UI matches Figma layout (with day chips instead of frequency select, plus optional description).
- New habits persist icon, color, reminder time, and `remoteId = null`.
- Home, Stats, and Detail show the stored icon/color.
- App still works offline from Room after create.
