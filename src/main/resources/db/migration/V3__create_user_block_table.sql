CREATE TABLE user_block (
    id UUID PRIMARY KEY,
    blocker_id VARCHAR(120) NOT NULL REFERENCES app_user (user_id) ON DELETE CASCADE,
    blocked_id VARCHAR(120) NOT NULL REFERENCES app_user (user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_user_block_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_block_not_self CHECK (blocker_id <> blocked_id)
);

CREATE INDEX idx_user_block_blocker
    ON user_block (blocker_id);

CREATE INDEX idx_user_block_blocked
    ON user_block (blocked_id);
