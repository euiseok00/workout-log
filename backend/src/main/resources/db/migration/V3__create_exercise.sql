CREATE TABLE exercise (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    body_part VARCHAR(20) NOT NULL,
    user_id INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_exercise_body_part CHECK (body_part IN ('BACK', 'CHEST', 'ARMS', 'LEGS', 'SHOULDERS')),
    CONSTRAINT fk_exercise_user FOREIGN KEY (user_id) REFERENCES users (id)
);
