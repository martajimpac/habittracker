create or replace function public.username_availability(requested text)
returns table (
  normalized text,
  available boolean,
  suggestions text[]
)
language plpgsql
security definer
set search_path = public
as $$
declare
  cleaned text;
  is_available boolean;
  options text[] := array[]::text[];
  candidate text;
  n int := 0;
begin
  cleaned := left(lower(regexp_replace(coalesce(requested, ''), '[^a-z0-9_]', '', 'g')), 30);
  if cleaned !~ '^[a-z0-9_]{3,30}$' then
    return query select cleaned, false, options;
    return;
  end if;

  is_available := not exists (
    select 1 from public.profiles p where lower(p.username) = cleaned
  );

  if not is_available then
    while cardinality(options) < 3 and n < 40 loop
      n := n + 1;
      if n = 3 then
        candidate := left(cleaned, 24) || '_bloom';
      else
        candidate := left(cleaned, 27) || n::text;
      end if;
      if candidate ~ '^[a-z0-9_]{3,30}$'
         and not exists (select 1 from public.profiles p where lower(p.username) = candidate)
         and not (candidate = any(options)) then
        options := options || candidate;
      end if;
    end loop;
  end if;

  return query select cleaned, is_available, options;
end;
$$;

revoke all on function public.username_availability(text) from public;
revoke all on function public.username_availability(text) from anon, authenticated;
grant execute on function public.username_availability(text) to anon, authenticated;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  requested text;
  base_username text;
  final_username text;
  suffix int := 0;
begin
  requested := left(
    lower(regexp_replace(coalesce(new.raw_user_meta_data ->> 'username', ''), '[^a-z0-9_]', '', 'g')),
    30
  );

  if requested ~ '^[a-z0-9_]{3,30}$'
     and not exists (select 1 from public.profiles p where lower(p.username) = requested) then
    final_username := requested;
  else
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
  end if;

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

revoke all on function public.handle_new_user() from public;
revoke all on function public.handle_new_user() from anon, authenticated;
