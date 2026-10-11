create table public.device_tokens (
  token text primary key,
  user_id uuid not null references public.profiles (id) on delete cascade,
  updated_at timestamptz not null default now()
);

create index device_tokens_user_idx on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;

grant delete on public.device_tokens to authenticated;

create policy device_tokens_delete_own on public.device_tokens
  for delete to authenticated
  using ((select auth.uid()) = user_id);
