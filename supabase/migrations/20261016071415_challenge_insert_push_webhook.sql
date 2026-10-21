create or replace function public.enqueue_challenge_push()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  webhook_secret text;
begin
  select decrypted_secret into webhook_secret
  from vault.decrypted_secrets
  where name = 'challenge_webhook_secret'
  limit 1;

  if webhook_secret is null or webhook_secret = '' then
    raise warning 'challenge webhook secret missing';
    return new;
  end if;

  perform net.http_post(
    url := 'https://sgmomulixnsugiwlkqgn.supabase.co/functions/v1/notify-challenge',
    body := jsonb_build_object(
      'type', 'INSERT',
      'table', 'challenges',
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

revoke all on function public.enqueue_challenge_push() from public;
revoke all on function public.enqueue_challenge_push() from anon;
revoke all on function public.enqueue_challenge_push() from authenticated;

drop trigger if exists challenges_insert_push on public.challenges;

create trigger challenges_insert_push
after insert on public.challenges
for each row
execute function public.enqueue_challenge_push();
