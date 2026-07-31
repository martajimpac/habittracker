-- revoke_handle_new_user_execute
-- Trigger-only: clients must not call handle_new_user via RPC

revoke all on function public.handle_new_user() from public;
revoke all on function public.handle_new_user() from anon, authenticated;
