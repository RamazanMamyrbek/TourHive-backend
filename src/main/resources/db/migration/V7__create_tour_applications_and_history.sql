CREATE TABLE tour_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    applicant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    contact_info VARCHAR(255),
    rejection_reason TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ensure a user can only have one active (PENDING or ACCEPTED) application per tour
CREATE UNIQUE INDEX idx_tour_apps_unique_active
ON tour_applications(tour_id, applicant_id)
WHERE status IN ('PENDING', 'ACCEPTED');

CREATE INDEX idx_tour_apps_tour_status ON tour_applications(tour_id, status);
CREATE INDEX idx_tour_apps_applicant ON tour_applications(applicant_id, created_at DESC);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES tour_applications(id) ON DELETE CASCADE,
    old_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    changed_by_id UUID NOT NULL REFERENCES users(id),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_history_application ON application_status_history(application_id, created_at ASC);
