# Habit Sync Supabase Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace the incomplete Supabase `habits` table with a UUID-aligned schema (`habits` + `habit_records` + RLS), cache habits in Room for offline **reads**, and implement **online-first** mutations (network required; Supabase then Room).

**Architecture:** Supabase is the source of truth for writes. Before create/update/toggle/delete, check connectivity; if offline, return a domain/UI error (`strings.xml` “no internet”) and do **not** touch Room. If online: write Supabase first; on success update Room; on remote failure `Log.e` + UI error and leave Room unchanged. UI still observes Room Flows. On authenticated login/app start, pull remote → upsert/replace Room cache (remote wins).

**Tech Stack:** Kotlin, Hilt, Room, Supabase Kotlin (Auth + Postgrest), ConnectivityManager / NetworkCallback, kotlinx.serialization, Coroutines/Flow

**Platform:** Android

**Spec:** `spec.md` → *Sync de habitos (Supabase)* (online-first)

**Commits:** Only when the user explicitly asks; stage task files at end of each task.

---

## Dependency graph

```text
Task 1 (Supabase SQL)
Task 2 (NetworkChecker + no-internet string) ──┐
Task 3 (Room cache fields) ────────────────────┼──► Task 4 (DTOs + HabitRemoteDataSource)
                                               ├──► Task 5 (Repository online-first writes)
                                               └──► Task 6 (Pull cache on login/start)
                                                      └──► Task 7 (Verify)
```

Tasks 1–3 can proceed in parallel once started. 4 needs schema (1). 5 needs 2–4. 6 needs 4–5.

---

## File map

| Action | Path |
|--------|------|
| Apply | MCP migration `replace_habits_sync_schema` (+ optional `supabase/migrations/…sql`) |
| Create | `core/network/NetworkChecker.kt` (or `data/remote/NetworkChecker.kt`) |
| Create | `data/remote/dto/HabitDto.kt`, `HabitRecordDto.kt` |
| Create | `data/remote/HabitRemoteDataSource.kt` |
| Create | `domain/usecase/SyncHabits.kt` (pull → Room) |
| Modify | Room entities/DAO/DB/mapper; `HabitRepository` / impl (mutation results) |
| Modify | `AddContentViewModel`, Home toggle VM path, delete path — surface no-internet / remote errors |
| Modify | `AppStartViewModel`, `LoginViewModel` — trigger pull when authenticated |
| Modify | `strings.xml` — `error_no_internet`, optional `error_sync_failed` |
| Test | online-first repository / use-case tests with fake remote + fake network |

---

### Task 1: Replace Supabase schema + RLS

**Files:** MCP `apply_migration` name `replace_habits_sync_schema`

- [ ] **Step 1: Apply SQL** (same schema as previously designed)

```sql
drop table if exists public.habits cascade;

create table public.habits (
  id uuid primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  description text,
  days_of_week smallint[] not null
    check (days_of_week <@ array[1,2,3,4,5,6,7]::smallint[]),
  icon text not null default '💧',
  color_hex text not null default '#6750A4',
  reminder_time text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table public.habit_records (
  id uuid primary key,
  habit_id uuid not null references public.habits (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  date date not null,
  is_completed boolean not null default false,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  unique (habit_id, date)
);

create index habits_user_id_idx on public.habits (user_id);
create index habits_user_updated_idx on public.habits (user_id, updated_at);
create index habit_records_habit_id_idx on public.habit_records (habit_id);
create index habit_records_user_updated_idx on public.habit_records (user_id, updated_at);

alter table public.habits enable row level security;
alter table public.habit_records enable row level security;

create policy habits_select_own on public.habits for select to authenticated
  using ((select auth.uid()) = user_id);
create policy habits_insert_own on public.habits for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy habits_update_own on public.habits for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy habits_delete_own on public.habits for delete to authenticated
  using ((select auth.uid()) = user_id);

create policy habit_records_select_own on public.habit_records for select to authenticated
  using ((select auth.uid()) = user_id);
create policy habit_records_insert_own on public.habit_records for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy habit_records_update_own on public.habit_records for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy habit_records_delete_own on public.habit_records for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.habits to authenticated;
grant select, insert, update, delete on public.habit_records to authenticated;
```

- [ ] **Step 2:** `list_tables` verbose + `get_advisors` security
- [ ] **Step 3:** Stage optional local SQL mirror

---

### Task 2: Network check + user-facing no-internet string

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/core/network/NetworkChecker.kt`
- Modify: `strings.xml` — `error_no_internet` (English, e.g. `No internet connection. Try again when you are online.`)
- Modify: DI provide/bind `NetworkChecker`
- Manifest: `ACCESS_NETWORK_STATE` if not already present

```kotlin
interface NetworkChecker {
    fun isOnline(): Boolean
}

class AndroidNetworkChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkChecker {
    override fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
```

- [ ] **Step 1:** Add string + permission if needed
- [ ] **Step 2:** Implement + bind `NetworkChecker`
- [ ] **Step 3:** Stage

---

### Task 3: Room cache fields aligned with remote

**Files:** `HabitEntity`, `HabitRecordEntity`, `HabitDatabase` (bump version), `HabitDao`, `HabitMapper`, call sites / tests

- Add `updatedAt: Long`, `deletedAt: Long?` on habits (and records as needed)
- Habit record PK: prefer `String` UUID `id` (align with remote) — update fakes/tests
- Reads: `WHERE deletedAt IS NULL`
- `DatabaseModule` already uses destructive fallback — OK for this branch
- Toggle uncomplete: keep row, set `isCompleted = false`, bump `updatedAt` (UNIQUE habitId+date)

- [ ] **Step 1:** Entities + version + DAO filters
- [ ] **Step 2:** Fix mapper / SaveHabit / tests compile
- [ ] **Step 3:** `./gradlew :app:compileDebugKotlin` → SUCCESS
- [ ] **Step 4:** Stage

---

### Task 4: DTOs + `HabitRemoteDataSource`

**Files:**
- `data/remote/dto/HabitDto.kt`, `HabitRecordDto.kt`
- `data/remote/HabitRemoteDataSource.kt`

Methods: `upsertHabit`, `upsertRecord`, `softDeleteHabit` / `deleteHabit`, `fetchHabits`, `fetchRecords`.  
`Log.e(TAG, msg, e)` on failures; never log tokens.  
Map days `1=Mon…7=Sun`.

- [ ] **Step 1–2:** DTOs + data source
- [ ] **Step 3:** Compile
- [ ] **Step 4:** Stage

---

### Task 5: Online-first repository mutations + UI errors

**Files:**
- `HabitRepository` / `HabitRepositoryImpl` — mutations return `DataResult` or throw typed errors (`NoInternet`, `RemoteFailure`); **never** write Room if offline or remote failed
- Order: `if (!networkChecker.isOnline()) return Error(NoInternet)` → remote → Room
- `AddContentViewModel`, Home toggle, delete flows — map to `R.string.error_no_internet` / sync failed
- Fake network + fake remote unit tests

```kotlin
// Pseudocode insert
suspend fun insertHabit(habit: Habit): DataResult<Unit> {
    if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network) // or dedicated NoInternet
    return try {
        remote.upsertHabit(habit.toDto(userId))
        habitDao.insertHabit(habit.toEntity())
        DataResult.Success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "insertHabit remote failed", e)
        DataResult.Error(AppError.Common.Unknown)
    }
}
```

- [ ] **Step 1:** Change repository contract + impl
- [ ] **Step 2:** Wire ViewModels / screens to show errors
- [ ] **Step 3:** Unit tests — offline ⇒ no DAO write; remote fail ⇒ no DAO write; success ⇒ DAO called
- [ ] **Step 4:** Stage

---

### Task 6: Pull cache on login / authenticated start

**Files:**
- `SyncHabits` use case: fetch remote → clear or upsert into Room (remote wins; simplest: upsert all non-deleted remote rows; remove local habits not present remotely for this user if feasible)
- `AppStartViewModel` / `LoginViewModel`: after authenticated, launch sync (don’t block navigation forever; log failures)

- [ ] **Step 1:** Implement `SyncHabits`
- [ ] **Step 2:** Trigger from start + login
- [ ] **Step 3:** Stage

---

### Task 7: Verification

- [ ] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` → SUCCESS
- [ ] MCP `get_advisors` security
- [ ] Manual: offline create → error, Room unchanged; online create → row in Supabase + Room; kill app offline → list still shows cache
- [ ] Stage leftovers

---

## Spec coverage

| Requirement | Task |
|-------------|------|
| Schema + RLS | 1 |
| Read offline from Room | 3, 6 |
| Writes require network + error string | 2, 5 |
| Supabase then Room | 5 |
| Pull on login/start | 6 |
| Logging on remote failure | 4, 5 |
| No offline mutation queue | (non-goal) |

## Placeholder scan

No TBD. Online-first write order and offline read-only are explicit.
