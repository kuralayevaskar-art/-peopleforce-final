-- Keep demo Docker/local environments usable with the documented password.
UPDATE users
SET password_hash = '{noop}Admin123!',
    status = 'ACTIVE',
    failed_login_attempts = 0,
    locked_at = NULL
WHERE email IN (
    'admin@demo.com',
    'hr@demo.com',
    'manager@demo.com',
    'employee@demo.com',
    'director@demo.com'
);
