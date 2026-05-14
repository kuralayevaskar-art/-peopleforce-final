ALTER TABLE registration_requests ADD COLUMN department VARCHAR(255);
UPDATE registration_requests SET department = 'DMUK' WHERE department IS NULL OR department = '';
ALTER TABLE registration_requests ALTER COLUMN department SET NOT NULL;
