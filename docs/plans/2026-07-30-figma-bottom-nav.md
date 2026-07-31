# Figma Bottom Navigation Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans` / subagents as needed.

**Goal:** Match Figma Make bottom bar: Home · Stats · elevated New FAB · Friends · Profile (labels + selection pill). Friends tab = placeholder screen for now.

**Platform:** Android / Compose

**Approved:** User said include Friends (2026-07-30).

---

- [x] Update `spec.md` bottom nav + Friends placeholder notes
- [x] Add strings: `tab_friends`, `tab_new`, `friends_title`, `friends_placeholder`
- [x] Add `TabScreens.TabFriends`; route in `NavigationBottomWrapper`
- [x] Create `FriendsScreen` / `FriendsContent` (Screen/Content pattern, placeholder UI)
- [x] Update `BottomNavigation` + restyle `HabitBottomBar` (Figma layout, FAB → `TabAddContent`)
- [x] Hide bottom bar on AddContent / Detail overlays
- [x] Compile + stage
