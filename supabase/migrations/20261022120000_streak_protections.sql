alter table public.habits
    add column if not exists schedule_mode text not null default '';

alter table public.habits
    drop constraint if exists habits_schedule_mode_check;

alter table public.habits
    add constraint habits_schedule_mode_check
        check (schedule_mode in ('', 'daily', 'weekdays', 'weekly_count'));

update public.habits
set schedule_mode = case
    when cardinality(days_of_week) between 1 and 6 then 'weekdays'
    when days_per_week between 1 and 6 then 'weekly_count'
    else 'daily'
end
where schedule_mode = '';

create table if not exists public.streak_protection_events (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    habit_id uuid not null,
    occasion_key text not null,
    event_type text not null check (event_type in ('GRANT', 'CONSUME')),
    created_at timestamptz not null default now(),
    unique (user_id, habit_id, occasion_key, event_type)
);

alter table public.streak_protection_events enable row level security;

drop policy if exists streak_protection_events_select_own on public.streak_protection_events;
create policy streak_protection_events_select_own
    on public.streak_protection_events
    for select
    using (auth.uid() = user_id);

drop policy if exists streak_protection_events_insert_own on public.streak_protection_events;
create policy streak_protection_events_insert_own
    on public.streak_protection_events
    for insert
    with check (auth.uid() = user_id);

create or replace function public.streak_protection_guard()
returns trigger
language plpgsql
as $$
declare
    balance integer;
begin
    select coalesce(count(*) filter (where event_type = 'GRANT'), 0)
         - coalesce(count(*) filter (where event_type = 'CONSUME'), 0)
        into balance
    from public.streak_protection_events
    where user_id = new.user_id;

    if new.event_type = 'GRANT' and balance >= 3 then
        return null;
    end if;
    if new.event_type = 'CONSUME' and balance <= 0 then
        return null;
    end if;
    return new;
end;
$$;

drop trigger if exists streak_protection_guard on public.streak_protection_events;
create trigger streak_protection_guard
    before insert on public.streak_protection_events
    for each row
    execute function public.streak_protection_guard();
