CREATE TABLE app_user (
    user_id VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
