CREATE TABLE conversation (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    type VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE conversation_member (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    user_id VARCHAR(120) NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_conversation_member UNIQUE (conversation_id, user_id)
);

CREATE TABLE chat_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    sender_id VARCHAR(120) NOT NULL,
    client_message_id VARCHAR(120),
    body VARCHAR(4000) NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_chat_message_client_id UNIQUE (conversation_id, sender_id, client_message_id)
);

CREATE INDEX idx_chat_message_conversation_time
    ON chat_message (conversation_id, created_at DESC);

CREATE INDEX idx_conversation_member_user
    ON conversation_member (user_id);
