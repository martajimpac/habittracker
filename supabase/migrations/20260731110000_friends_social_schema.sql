-- friends_social_schema
-- profiles + friendships + habits.is_public + challenges + RLS

create schema if not exists private;

-- ── profiles ─────────────────────────────────────────────────────────────────
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  username text not null,
  display_name text not null,
  avatar_color text not null default '#6750A4',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profiles_username_format check (username ~ '^[a-z0-9_]{3,30}$')
);

create unique index profiles_username_uidx on public.profiles (lower(username));

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  base_username text;
  final_username text;
  suffix int := 0;
begin
  base_username := lower(regexp_replace(
    coalesce(nullif(split_part(new.email, '@', 1), ''), 'user'),
    '[^a-z0-9_]',
    '',
    'g'
  ));
  if length(base_username) < 3 then
    base_username := 'user' || substr(replace(new.id::text, '-', ''), 1, 6);
  end if;
  if length(base_username) > 24 then
    base_username := left(base_username, 24);
  end if;

  final_username := base_username;
  while exists (select 1 from public.profiles p where lower(p.username) = final_username) loop
    suffix := suffix + 1;
    final_username := left(base_username, 24) || suffix::text;
  end loop;

  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    final_username,
    coalesce(
      nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
      nullif(split_part(new.email, '@', 1), ''),
      'User'
    )
  );
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

insert into public.profiles (id, username, display_name)
select
  u.id,
  left(
    lower(regexp_replace(coalesce(nullif(split_part(u.email, '@', 1), ''), 'user'), '[^a-z0-9_]', '', 'g'))
      || substr(replace(u.id::text, '-', ''), 1, 4),
    30
  ),
  coalesce(
    nullif(trim(u.raw_user_meta_data ->> 'name'), ''),
    nullif(split_part(u.email, '@', 1), ''),
    'User'
  )
from auth.users u
on conflict (id) do nothing;

alter table public.profiles enable row level security;

create policy profiles_select_authenticated on public.profiles
  for select to authenticated
  using (true);

create policy profiles_update_own on public.profiles
  for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

grant select, update on public.profiles to authenticated;

-- ── friendships ──────────────────────────────────────────────────────────────
create table public.friendships (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid not null references public.profiles (id) on delete cascade,
  addressee_id uuid not null references public.profiles (id) on delete cascade,
  status text not null default 'pending'
    check (status in ('pending', 'accepted', 'rejected')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint friendships_not_self check (requester_id <> addressee_id)
);

create unique index friendships_pair_uidx on public.friendships (
  least(requester_id, addressee_id),
  greatest(requester_id, addressee_id)
);

create index friendships_requester_idx on public.friendships (requester_id);
create index friendships_addressee_idx on public.friendships (addressee_id);
create index friendships_status_idx on public.friendships (status);

alter table public.friendships enable row level security;

create policy friendships_select_participants on public.friendships
  for select to authenticated
  using (
    (select auth.uid()) = requester_id
    or (select auth.uid()) = addressee_id
  );

create policy friendships_insert_as_requester on public.friendships
  for insert to authenticated
  with check (
    (select auth.uid()) = requester_id
    and status = 'pending'
  );

create policy friendships_update_addressee_or_requester on public.friendships
  for update to authenticated
  using (
    (select auth.uid()) = addressee_id
    or (select auth.uid()) = requester_id
  )
  with check (
    (select auth.uid()) = addressee_id
    or (select auth.uid()) = requester_id
  );

create policy friendships_delete_participants on public.friendships
  for delete to authenticated
  using (
    (select auth.uid()) = requester_id
    or (select auth.uid()) = addressee_id
  );

grant select, insert, update, delete on public.friendships to authenticated;

-- ── habits.is_public ─────────────────────────────────────────────────────────
alter table public.habits
  add column if not exists is_public boolean not null default false;

create index if not exists habits_is_public_idx on public.habits (user_id)
  where is_public = true and deleted_at is null;

-- ── challenges ───────────────────────────────────────────────────────────────
create table public.challenges (
  id uuid primary key default gen_random_uuid(),
  challenger_id uuid not null references public.profiles (id) on delete cascade,
  challenged_id uuid not null references public.profiles (id) on delete cascade,
  challenger_habit_id uuid not null references public.habits (id) on delete cascade,
  challenged_habit_id uuid not null references public.habits (id) on delete cascade,
  criteria text not null
    check (criteria in ('streak', 'all_days', 'completion_pct')),
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  status text not null default 'pending'
    check (status in ('pending', 'active', 'completed', 'declined', 'cancelled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint challenges_not_self check (challenger_id <> challenged_id),
  constraint challenges_time_range check (ends_at > starts_at)
);

create index challenges_challenger_idx on public.challenges (challenger_id);
create index challenges_challenged_idx on public.challenges (challenged_id);
create index challenges_status_idx on public.challenges (status);
create index challenges_habit_challenger_idx on public.challenges (challenger_habit_id);
create index challenges_habit_challenged_idx on public.challenges (challenged_habit_id);

alter table public.challenges enable row level security;

grant select, insert, update on public.challenges to authenticated;

-- ── RLS helpers (after tables exist) ─────────────────────────────────────────
create or replace function private.is_friend_with(other_user uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.friendships f
    where f.status = 'accepted'
      and (
        (f.requester_id = (select auth.uid()) and f.addressee_id = other_user)
        or (f.addressee_id = (select auth.uid()) and f.requester_id = other_user)
      )
  );
$$;

create or replace function private.shares_active_challenge_habit(target_habit_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.challenges c
    where c.status in ('pending', 'active')
      and (
        c.challenger_id = (select auth.uid())
        or c.challenged_id = (select auth.uid())
      )
      and (
        c.challenger_habit_id = target_habit_id
        or c.challenged_habit_id = target_habit_id
      )
  );
$$;

revoke all on function private.is_friend_with(uuid) from public;
revoke all on function private.shares_active_challenge_habit(uuid) from public;
grant execute on function private.is_friend_with(uuid) to authenticated;
grant execute on function private.shares_active_challenge_habit(uuid) to authenticated;

-- ── habits / challenges / habit_records policies ─────────────────────────────
drop policy if exists habits_select_own on public.habits;

create policy habits_select_own_or_friend_public on public.habits
  for select to authenticated
  using (
    (select auth.uid()) = user_id
    or (
      is_public = true
      and deleted_at is null
      and private.is_friend_with(user_id)
    )
    or private.shares_active_challenge_habit(id)
  );

create policy challenges_select_participants on public.challenges
  for select to authenticated
  using (
    (select auth.uid()) = challenger_id
    or (select auth.uid()) = challenged_id
  );

create policy challenges_insert_as_challenger on public.challenges
  for insert to authenticated
  with check (
    (select auth.uid()) = challenger_id
    and private.is_friend_with(challenged_id)
    and exists (
      select 1 from public.habits h
      where h.id = challenger_habit_id
        and h.user_id = challenger_id
        and h.deleted_at is null
    )
    and exists (
      select 1 from public.habits h
      where h.id = challenged_habit_id
        and h.user_id = challenged_id
        and h.deleted_at is null
    )
  );

create policy challenges_update_participants on public.challenges
  for update to authenticated
  using (
    (select auth.uid()) = challenger_id
    or (select auth.uid()) = challenged_id
  )
  with check (
    (select auth.uid()) = challenger_id
    or (select auth.uid()) = challenged_id
  );

drop policy if exists habit_records_select_own on public.habit_records;

create policy habit_records_select_own_or_social on public.habit_records
  for select to authenticated
  using (
    (select auth.uid()) = user_id
    or exists (
      select 1
      from public.habits h
      where h.id = habit_id
        and h.is_public = true
        and h.deleted_at is null
        and private.is_friend_with(h.user_id)
    )
    or private.shares_active_challenge_habit(habit_id)
  );
