# Friends Social Schema (Supabase) Implementation Plan

> **For agentic workers:** Apply migration via Supabase MCP `apply_migration`; keep SQL mirrored under `supabase/migrations/`.

**Goal:** Create Supabase schema for Figma Friends: profiles, friendships (request/accept), `habits.is_public`, challenges; RLS + grants.

**Architecture:** Approach A from approved design. Progress computed client-side from `habit_records`. Helper `private.is_friend_with` (SECURITY DEFINER) for RLS without recursion.

**Platform:** Android / Supabase Postgres

---

- [x] Spec + brain decision approved
- [x] Write migration SQL + apply remotely
- [x] Verify `list_tables` + advisors
- [x] Stage migration file + docs
