INSERT INTO departments (id, company_id, name, description, status)
VALUES ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000001', 'HR', 'Human Resources Department', 'ACTIVE');

INSERT INTO departments (id, company_id, name, description, status)
VALUES ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000001', 'IT', 'Information Technology Department', 'ACTIVE');

INSERT INTO departments (id, company_id, name, description, status)
VALUES ('00000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000001', 'Sales', 'Sales and Marketing Department', 'ACTIVE');

INSERT INTO departments (id, company_id, name, description, status)
VALUES ('00000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000001', 'Finance', 'Finance and Accounting Department', 'ACTIVE');

INSERT INTO positions (id, company_id, department_id, name, level, status)
VALUES ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000201', 'HR Manager', 'Senior', 'ACTIVE');

INSERT INTO positions (id, company_id, department_id, name, level, status)
VALUES ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000202', 'Software Engineer', 'Middle', 'ACTIVE');

INSERT INTO positions (id, company_id, department_id, name, level, status)
VALUES ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000203', 'Sales Manager', 'Junior', 'ACTIVE');

INSERT INTO positions (id, company_id, department_id, name, level, status)
VALUES ('00000000-0000-0000-0000-000000000304', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000204', 'Accountant', 'Senior', 'ACTIVE');

INSERT INTO employees (id, company_id, department_id, position_id, first_name, last_name, email, status)
VALUES ('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000301', 'HR', 'Admin', 'hr@demo.com', 'ACTIVE');

INSERT INTO employees (id, company_id, department_id, position_id, first_name, last_name, email, status)
VALUES ('00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000302', 'John', 'Developer', 'manager@demo.com', 'ACTIVE');

INSERT INTO employees (id, company_id, department_id, position_id, first_name, last_name, email, status)
VALUES ('00000000-0000-0000-0000-000000000404', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000302', 'Jane', 'Doe', 'employee@demo.com', 'ACTIVE');

UPDATE users SET employee_id = '00000000-0000-0000-0000-000000000402' WHERE email = 'hr@demo.com';
UPDATE users SET employee_id = '00000000-0000-0000-0000-000000000403' WHERE email = 'manager@demo.com';
UPDATE users SET employee_id = '00000000-0000-0000-0000-000000000404' WHERE email = 'employee@demo.com';
