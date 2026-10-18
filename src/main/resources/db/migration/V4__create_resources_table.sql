-- V4: Resources (Media and file storage metadata) and User Profile avatar relationship

CREATE TABLE resources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    key VARCHAR(500) NOT NULL UNIQUE,
    folder VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resources_folder ON resources(folder);

ALTER TABLE user_profiles ADD COLUMN avatar_resource_id UUID REFERENCES resources(id) ON DELETE SET NULL;
CREATE INDEX idx_user_profiles_avatar_resource ON user_profiles(avatar_resource_id);
