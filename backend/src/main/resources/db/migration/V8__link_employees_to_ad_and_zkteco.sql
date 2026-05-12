ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS ad_username VARCHAR(255),
    ADD COLUMN IF NOT EXISTS zkteco_pin VARCHAR(255),
    ADD COLUMN IF NOT EXISTS zkteco_person_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_employees_ad_username ON employees (ad_username);
CREATE INDEX IF NOT EXISTS idx_employees_zkteco_pin ON employees (zkteco_pin);
CREATE UNIQUE INDEX IF NOT EXISTS idx_attendance_logs_external_event_id
    ON attendance_logs (external_event_id)
    WHERE external_event_id IS NOT NULL;
