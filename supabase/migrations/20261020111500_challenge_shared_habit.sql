-- A new challenge starts without the friend's habit. That copy is created when they accept.

alter table public.challenges
  alter column challenged_habit_id drop not null;

drop policy if exists challenges_insert_as_challenger on public.challenges;

create policy challenges_insert_as_challenger on public.challenges
  for insert to authenticated
  with check (
    (select auth.uid()) = challenger_id
    and private.is_friend_with(challenged_id)
    and challenged_habit_id is null
    and exists (
      select 1
      from public.habits h
      where h.id = challenger_habit_id
        and h.user_id = challenger_id
        and h.deleted_at is null
        and h.is_public = true
    )
  );

drop policy if exists challenges_update_participants on public.challenges;

create policy challenges_update_participants on public.challenges
  for update to authenticated
  using (
    (select auth.uid()) = challenger_id
    or (select auth.uid()) = challenged_id
  )
  with check (
    (
      (select auth.uid()) = challenger_id
      or (select auth.uid()) = challenged_id
    )
    and (
      challenged_habit_id is null
      or exists (
        select 1
        from public.habits h
        where h.id = challenged_habit_id
          and h.user_id = challenged_id
          and h.deleted_at is null
      )
    )
  );
