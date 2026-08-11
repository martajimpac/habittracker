-- replace_habits_sync_schema
-- Applied via Supabase MCP 2026-07-23

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
