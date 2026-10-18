CREATE TABLE routine_set (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_in_routine_id INTEGER NOT NULL REFERENCES exercise_in_routine (id),
    set_number INTEGER NOT NULL CHECK (set_number >= 1),
    weight NUMERIC(6,2) NOT NULL CHECK (weight >= 0),
    reps INTEGER NOT NULL CHECK (reps >= 0),
    CONSTRAINT uq_routine_set_number UNIQUE (exercise_in_routine_id, set_number)
);

ALTER TABLE exercise_in_routine DROP CONSTRAINT uq_exercise_in_routine_exercise;
