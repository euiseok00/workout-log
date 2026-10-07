CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_login_id UNIQUE (login_id),
    CONSTRAINT ck_users_login_id_format CHECK (login_id ~ '^[a-z0-9_]{4,20}$')
);
