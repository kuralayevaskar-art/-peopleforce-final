ALTER TABLE registration_requests ADD COLUMN first_name VARCHAR(255);
ALTER TABLE registration_requests ADD COLUMN last_name VARCHAR(255);

UPDATE registration_requests
SET first_name = split_part(full_name, ' ', 1),
    last_name = NULLIF(trim(substr(full_name, length(split_part(full_name, ' ', 1)) + 1)), '')
WHERE first_name IS NULL OR last_name IS NULL;

UPDATE registration_requests
SET last_name = first_name
WHERE last_name IS NULL OR last_name = '';

ALTER TABLE registration_requests ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE registration_requests ALTER COLUMN last_name SET NOT NULL;
