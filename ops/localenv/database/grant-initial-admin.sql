-- Administrator-approved bootstrap only. psql -v admin_email='actual-address'
-- Register the user through the API first. Existing hash/credentials are not copied.
INSERT INTO public.user_roles (assigned_at, role_id, user_id)
SELECT now(), r.id, u.user_id FROM public.users u CROSS JOIN public.roles r
WHERE u.email = :'admin_email' AND u.deleted_at IS NULL AND r.role_key = 'SYS_ADMIN'
AND NOT EXISTS (SELECT 1 FROM public.user_roles ur WHERE ur.user_id=u.user_id AND ur.role_id=r.id);
