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
