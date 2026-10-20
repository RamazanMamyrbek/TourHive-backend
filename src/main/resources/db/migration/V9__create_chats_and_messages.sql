-- V9: Create chats, chat_members, messages, and message_reads tables

CREATE TABLE chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(32) NOT NULL,
    tour_id UUID REFERENCES tours(id) ON DELETE CASCADE,
    title VARCHAR(255),
    avatar_url VARCHAR(500),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    user_id_1 UUID REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_chats_direct_pair CHECK (
        type != 'DIRECT' OR (user_id_1 IS NOT NULL AND user_id_2 IS NOT NULL AND user_id_1 < user_id_2)
    ),
    CONSTRAINT uq_chats_direct_pair UNIQUE (user_id_1, user_id_2),
    CONSTRAINT uq_chats_tour_id UNIQUE (tour_id)
);

CREATE INDEX idx_chats_type ON chats(type);
CREATE INDEX idx_chats_tour_id ON chats(tour_id);
CREATE INDEX idx_chats_updated_at ON chats(updated_at DESC);

CREATE TABLE chat_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    left_at TIMESTAMP WITH TIME ZONE,
    last_read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_members_chat_user UNIQUE (chat_id, user_id)
);

CREATE INDEX idx_chat_members_user_active ON chat_members(user_id, is_active);
CREATE INDEX idx_chat_members_chat_id ON chat_members(chat_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    content TEXT,
    attachment_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_chat_created ON messages(chat_id, created_at DESC);
CREATE INDEX idx_messages_sender ON messages(sender_id);

CREATE TABLE message_reads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_reads_message_user UNIQUE (message_id, user_id)
);

CREATE INDEX idx_message_reads_user_chat ON message_reads(user_id, chat_id);
CREATE INDEX idx_message_reads_message_id ON message_reads(message_id);
