# Login Card + Logo Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Restyle Login with gradient background, white form card, and shared `logo` drawable used in-screen and as launcher icon. No Remember me / social login.

**Scope:** UI only — no auth logic changes.

## Files

- Create: `app/src/main/res/drawable/logo.xml`
- Create/update: launcher background color + adaptive icon refs
- Modify: `LoginScreen.kt`, `spec.md`, `Color.kt` (login gradient colors if needed)
- Plan: this file

## Tasks

- [x] 1. Add `logo.xml` (white mark suitable for tint/gradient; readable on launcher with blue bg)
- [x] 2. Wire launcher (`ic_launcher` / round) to use `logo` as foreground + solid blue background
- [x] 3. Restyle `LoginScreen`: gradient bg, logo + app name, white card wrapping existing form fields/actions
- [x] 4. Update `spec.md` Login UI notes (done with approval)
- [x] 5. Compile `:app:compileDebugKotlin` and stage all task files

## Out of scope

- Remember me, Google/Facebook, Register screen redesign, Material→XML icon migration for email/lock
