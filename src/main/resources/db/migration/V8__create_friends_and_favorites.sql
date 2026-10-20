-- V8: Create friend_requests, friendships, and favorites tables

CREATE TABLE friend_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_friend_request_distinct_users CHECK (sender_id <> receiver_id)
);

CREATE UNIQUE INDEX idx_unique_pending_friend_request
ON friend_requests (sender_id, receiver_id)
WHERE status = 'PENDING';

CREATE INDEX idx_friend_requests_receiver_status ON friend_requests (receiver_id, status);
CREATE INDEX idx_friend_requests_sender_status ON friend_requests (sender_id, status);

CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id_1 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_friendship_canonical_order CHECK (user_id_1 < user_id_2),
    CONSTRAINT uq_friendship_pair UNIQUE (user_id_1, user_id_2)
);

CREATE INDEX idx_friendships_user1 ON friendships (user_id_1);
CREATE INDEX idx_friendships_user2 ON friendships (user_id_2);

CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tour_id UUID NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_tour_favorite UNIQUE (user_id, tour_id)
);

CREATE INDEX idx_favorites_user ON favorites (user_id, created_at DESC);
CREATE INDEX idx_favorites_tour ON favorites (tour_id);
