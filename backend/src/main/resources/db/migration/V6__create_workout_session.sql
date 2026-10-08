CREATE TABLE workout_session (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INTEGER NOT NULL,
    routine_id INTEGER NULL,
    workout_date DATE NOT NULL,
    CONSTRAINT fk_workout_session_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_workout_session_routine FOREIGN KEY (routine_id) REFERENCES routine (id)
);

CREATE TABLE exercise_in_session (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    exercise_order INTEGER NOT NULL,
    CONSTRAINT fk_exercise_in_session_session FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_exercise_in_session_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
    CONSTRAINT ck_exercise_in_session_order CHECK (exercise_order >= 1),
    CONSTRAINT uq_exercise_in_session_order UNIQUE (session_id, exercise_order)
);

CREATE TABLE exercise_set (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_in_session_id INTEGER NOT NULL,
    set_number INTEGER NOT NULL,
    weight NUMERIC(6,2) NOT NULL,
    reps INTEGER NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_exercise_set_exercise_in_session
        FOREIGN KEY (exercise_in_session_id) REFERENCES exercise_in_session (id),
    CONSTRAINT ck_exercise_set_number CHECK (set_number >= 1),
    CONSTRAINT ck_exercise_set_weight CHECK (weight >= 0),
    CONSTRAINT ck_exercise_set_reps CHECK (reps >= 0),
    CONSTRAINT uq_exercise_set_number UNIQUE (exercise_in_session_id, set_number)
);
