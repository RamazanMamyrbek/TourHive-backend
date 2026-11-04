-- V15: Create user_follows table for Instagram-style subscriptions and add post visibility

CREATE TABLE IF NOT EXISTS user_follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_follow_not_self CHECK (follower_id <> following_id),
    CONSTRAINT uq_user_follow UNIQUE (follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows (follower_id);
CREATE INDEX IF NOT EXISTS idx_user_follows_following ON user_follows (following_id);

-- Migrate existing mutual friendships to bidirectional follows
INSERT INTO user_follows (follower_id, following_id, created_at)
SELECT user_id_1, user_id_2, created_at FROM friendships
ON CONFLICT DO NOTHING;

INSERT INTO user_follows (follower_id, following_id, created_at)
SELECT user_id_2, user_id_1, created_at FROM friendships
ON CONFLICT DO NOTHING;

-- Add is_private flag to posts
ALTER TABLE posts ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_posts_is_private ON posts (is_private);
CREATE INDEX IF NOT EXISTS idx_posts_author_private ON posts (author_id, is_private);

