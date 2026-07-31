# Decisions

<!--
Architecture decisions specific to this project.
Append entries with: mobiai brain save decision
Each entry should record: title, status (active|deprecated), platform,
area, date, decision, reason, files.
-->

## ViewModels may use repositories; Compose screens may not

- id: viewmodels-may-use-repositories
- type: architecture_decision
- status: active
- platform: android
- area: architecture
- date: 2026-07-22

### Decision
Compose screens never access repositories directly; ViewModels may use domain repository interfaces (and Use Cases when present).

### Reason
Matches how the app already works (Home/AppStart/Profile ViewModels inject repos) and keeps Composables free of data-layer dependencies without forcing a Use Case for every screen.

### Alternatives considered
- UI never accesses repositories (including forbidding ViewModel → repo) — rejected; too strict vs current code and unwanted by the team
- Compose screens may inject repositories directly — rejected; blurs presentation boundaries

### Files
- AGENTS.md
- .mobiai/brain/config.json

## Offline-first habit sync dual-write + LWW

- id: offline-first-habit-sync-dual-write-lww
- type: architecture_decision
- status: deprecated
- platform: android
- area: sync_persistence
- date: 2026-07-23

### Decision
~~Habit sync uses offline-first dual-write…~~ **Deprecated 2026-07-23** — replaced by online-first (see below). Offline mutations risked being stranded or confusing vs pull; product chose to require network for all writes.

### Reason
(Original) UI already reads Room offline…

### Alternatives considered
(Original alternatives)

### Files
- spec.md
- app/src/main/java/com/marta/habittracker/data/repository/HabitRepositoryImpl.kt

## Online-first habit sync (read cache offline)

- id: online-first-habit-sync-read-cache
- type: architecture_decision
- status: active
- platform: android
- area: sync_persistence
- date: 2026-07-23

### Decision
Habit mutations (create, update, toggle complete, delete) require internet. Order: connectivity check → Supabase write → Room cache update. Without network, show a user-facing "no internet" error and do not change Room. Reads use Room cache and work offline. Pull on authenticated login/app start refreshes Room from Supabase (remote is source of truth). Same client UUID PK in Room and Supabase.

### Reason
Avoids lost/overwritten offline edit queues without building an outbox. Still allows browsing cached habits without connectivity.

### Alternatives considered
- Offline-first dual-write + LWW — rejected; pending local changes vs pull was confusing and needed push-after-merge
- Outbox + worker — more correct offline writes, deferred as unnecessary if writes require network
- Block reads offline too — rejected; user wants cached read-only offline

### Files
- spec.md
- docs/plans/2026-07-23-habit-sync-supabase.md
- app/src/main/java/com/marta/habittracker/data/repository/HabitRepositoryImpl.kt

## Friends tab MVP is UI-only empty state

- id: friends-tab-mvp-is-ui-only-empty-state-20260730-124350
- type: architecture_decision
- status: active
- platform: android
- area: friends_ui
- date: 2026-07-30

### Decision
Friends tab MVP is UI-only empty state: Figma Make header (title + compact gradient Add button with no-op click) and centered empty copy. No ViewModel, repository, Supabase, friend list, challenges, or bottom sheets.

### Reason
Product chose empty-state-first before social backend.

### Alternatives considered
- Full Figma friends list + sheets with mock data — deferred
- Pure centered empty without header Add — rejected
- Add bottom sheet UI without backend — deferred

## Friends social schema: profiles friendships challenges is_public

- id: friends-social-schema-profiles-friendships-challenges-is-pub-20260731-105933
- type: architecture_decision
- status: active
- platform: android
- area: supabase_social
- date: 2026-07-31

### Decision
Full Figma Friends backend on Supabase: profiles + friendships (request/accept) + habits.is_public + challenges with two habit FKs (same semantic habit). Progress computed from habit_records between starts_at/ends_at. RLS: owner CRUD; accepted friends SELECT public habits; challenge participants read peer records for progress.

### Reason
Matches Figma (friends list, public habits, active challenges, add-friend requests) without denormalized progress columns.

### Alternatives considered
- Instant friendship / follow — rejected
- Profile-level privacy only — rejected (per-habit)
- Stored progress columns — rejected (computed)
- Separate friend_requests table — deferred; single friendships.status is enough
