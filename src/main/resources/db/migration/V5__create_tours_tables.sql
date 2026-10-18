-- V5: Create tours, tour_countries, tour_cities, tour_includes, tour_route_items, and tour_images tables

CREATE TABLE tours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    tour_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    capacity INT NOT NULL,
    available_places INT NOT NULL,
    price NUMERIC(12, 2),
    currency VARCHAR(10) DEFAULT 'KZT',
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tour_countries (
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    country VARCHAR(100) NOT NULL,
    PRIMARY KEY (tour_id, country)
);

CREATE TABLE tour_cities (
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    city VARCHAR(100) NOT NULL,
    PRIMARY KEY (tour_id, city)
);

CREATE TABLE tour_includes (
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    include_item VARCHAR(100) NOT NULL,
    PRIMARY KEY (tour_id, include_item)
);

CREATE TABLE tour_route_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    city VARCHAR(100) NOT NULL,
    route_date DATE,
    description TEXT,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tour_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tours_organizer_id ON tours(organizer_id);
CREATE INDEX idx_tours_status ON tours(status);
CREATE INDEX idx_tour_route_tour_id ON tour_route_items(tour_id);
CREATE INDEX idx_tour_images_tour_id ON tour_images(tour_id);
