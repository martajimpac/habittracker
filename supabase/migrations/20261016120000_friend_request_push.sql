-- Reuses the challenge webhook secret already stored in Vault.
-- A missing secret warns and still lets the friendship insert succeed.

create or replace function public.enqueue_friend_request_push()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  webhook_secret text;
begin
  if new.status is distinct from 'pending' then
    return new;
  end if;

  select decrypted_secret into webhook_secret
  from vault.decrypted_secrets
  where name = 'challenge_webhook_secret'
  limit 1;

  if webhook_secret is null or webhook_secret = '' then
    raise warning 'friend request webhook secret missing';
    return new;
  end if;

  perform net.http_post(
    url := 'https://sgmomulixnsugiwlkqgn.supabase.co/functions/v1/notify-friend-request',
    body := jsonb_build_object(
      'type', 'INSERT',
      'table', 'friendships',
      'schema', 'public',
      'record', to_jsonb(new),
      'old_record', null
    ),
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-webhook-secret', webhook_secret
    ),
    timeout_milliseconds := 10000
  );

  return new;
end;
$$;

revoke all on function public.enqueue_friend_request_push() from public;
revoke all on function public.enqueue_friend_request_push() from anon;
revoke all on function public.enqueue_friend_request_push() from authenticated;

drop trigger if exists friendships_insert_push on public.friendships;

create trigger friendships_insert_push
  after insert on public.friendships
  for each row
  execute function public.enqueue_friend_request_push();
