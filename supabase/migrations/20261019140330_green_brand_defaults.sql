-- New rows fall back to the forest-green HabitPrimary. Existing colors stay as stored.
alter table public.profiles
  alter column avatar_color set default '#294536';

alter table public.habits
  alter column color_hex set default '#294536';
