alter table public.habits
    add column if not exists days_per_week smallint not null default 7;

alter table public.habits
    drop constraint if exists habits_days_per_week_check;

alter table public.habits
    add constraint habits_days_per_week_check check (days_per_week between 1 and 7);

update public.habits
set days_per_week = cardinality(days_of_week)
where days_of_week is not null
  and cardinality(days_of_week) between 1 and 6;
