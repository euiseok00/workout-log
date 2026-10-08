CREATE TABLE routine (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_routine_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE exercise_in_routine (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    exercise_order INTEGER NOT NULL,
    target_sets INTEGER NULL,
    target_reps INTEGER NULL,
    CONSTRAINT fk_exercise_in_routine_routine FOREIGN KEY (routine_id) REFERENCES routine (id),
    CONSTRAINT fk_exercise_in_routine_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
    CONSTRAINT uq_exercise_in_routine_order UNIQUE (routine_id, exercise_order),
    CONSTRAINT uq_exercise_in_routine_exercise UNIQUE (routine_id, exercise_id)
);
