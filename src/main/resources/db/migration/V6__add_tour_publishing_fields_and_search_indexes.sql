-- V6: Add tour publishing lifecycle fields and search performance indexes

ALTER TABLE tours
    ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
    ADD COLUMN IF NOT EXISTS reviewed_by_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_tours_status_start_date ON tours(status, start_date);
CREATE INDEX IF NOT EXISTS idx_tours_price ON tours(price);
CREATE INDEX IF NOT EXISTS idx_tour_countries_country ON tour_countries(country);
CREATE INDEX IF NOT EXISTS idx_tour_cities_city ON tour_cities(city);
