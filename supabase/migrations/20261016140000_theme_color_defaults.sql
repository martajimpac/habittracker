-- New rows fall back to HabitPrimary. Existing colors stay as stored.
alter table public.profiles
  alter column avatar_color set default '#021C4B';

alter table public.habits
  alter column color_hex set default '#021C4B';
