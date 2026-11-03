-- V14: Add user profile contacts, specialization, experience and certificates

-- 1. Extend user_profiles table with separated contact channels and professional info
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS telegram VARCHAR(100),
    ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(50),
    ADD COLUMN IF NOT EXISTS instagram VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(150),
    ADD COLUMN IF NOT EXISTS experience_years INTEGER,
    ADD COLUMN IF NOT EXISTS website VARCHAR(255);

-- 2. Create user_certificates table for licenses, diplomas, and credentials
CREATE TABLE IF NOT EXISTS user_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    issuer VARCHAR(255),
    issue_year VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_certificates_user_id ON user_certificates(user_id);

