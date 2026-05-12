INSERT INTO roles (code, name)
VALUES ('INTEGRATION_ADMIN', 'Integration Administrator')
ON CONFLICT (code) DO NOTHING;
