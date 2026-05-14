ALTER TABLE employees ADD COLUMN ad_username VARCHAR(255);
ALTER TABLE employees ADD COLUMN zkteco_pin VARCHAR(255);
ALTER TABLE employees ADD COLUMN zkteco_person_id VARCHAR(255);

CREATE TABLE audit_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID,
    actor_user_id UUID,
    actor_email VARCHAR(255),
    target_user_id UUID,
    target_employee_id UUID,
    action VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    error_message TEXT,
    ip_address VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE registration_links (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE registration_requests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    link_id UUID NOT NULL REFERENCES registration_links(id),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    personal_email VARCHAR(255) NOT NULL,
    identity_document_file_id VARCHAR(255) NOT NULL,
    face_photo_file_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    corporate_email VARCHAR(255),
    rejection_reason TEXT,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,
    rejected_by UUID REFERENCES users(id),
    rejected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
