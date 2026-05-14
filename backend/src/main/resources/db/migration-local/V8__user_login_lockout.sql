ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;
UPDATE users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;
