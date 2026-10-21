alter table public.habits
  add column if not exists reminder_enabled boolean not null default false;

update public.habits
set reminder_enabled = true
where reminder_time is not null
  and btrim(reminder_time) <> '';
