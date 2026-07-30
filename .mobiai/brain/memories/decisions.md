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
