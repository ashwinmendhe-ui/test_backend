-- New empty database only; no production users or password hashes.
INSERT INTO public.roles (id, created_at, description, role_key) VALUES
 (1, now(), 'System administrator', 'SYS_ADMIN'),
 (2, now(), 'Company administrator', 'COMPANY_ADMIN'),
 (3, now(), 'Company user', 'COMPANY_USER');
SELECT setval(pg_get_serial_sequence('public.roles','id'), (SELECT max(id) FROM public.roles));
