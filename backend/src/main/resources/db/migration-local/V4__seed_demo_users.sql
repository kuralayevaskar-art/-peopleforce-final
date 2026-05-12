-- Demo password is Admin123!
INSERT INTO users (id, company_id, email, password_hash, status)
VALUES ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'admin@demo.com', '{noop}Admin123!', 'ACTIVE');

INSERT INTO users (id, company_id, email, password_hash, status)
VALUES ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001', 'hr@demo.com', '{noop}Admin123!', 'ACTIVE');

INSERT INTO users (id, company_id, email, password_hash, status)
VALUES ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000001', 'manager@demo.com', '{noop}Admin123!', 'ACTIVE');

INSERT INTO users (id, company_id, email, password_hash, status)
VALUES ('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000001', 'employee@demo.com', '{noop}Admin123!', 'ACTIVE');

INSERT INTO users (id, company_id, email, password_hash, status)
VALUES ('00000000-0000-0000-0000-000000000105', '00000000-0000-0000-0000-000000000001', 'director@demo.com', '{noop}Admin123!', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id) VALUES ('00000000-0000-0000-0000-000000000101', (SELECT id FROM roles WHERE code = 'SUPER_ADMIN'));
INSERT INTO user_roles (user_id, role_id) VALUES ('00000000-0000-0000-0000-000000000102', (SELECT id FROM roles WHERE code = 'HR_ADMIN'));
INSERT INTO user_roles (user_id, role_id) VALUES ('00000000-0000-0000-0000-000000000103', (SELECT id FROM roles WHERE code = 'MANAGER'));
INSERT INTO user_roles (user_id, role_id) VALUES ('00000000-0000-0000-0000-000000000104', (SELECT id FROM roles WHERE code = 'EMPLOYEE'));
INSERT INTO user_roles (user_id, role_id) VALUES ('00000000-0000-0000-0000-000000000105', (SELECT id FROM roles WHERE code = 'DIRECTOR'));
