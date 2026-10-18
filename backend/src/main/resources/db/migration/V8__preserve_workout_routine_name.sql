ALTER TABLE workout_session ADD COLUMN routine_name VARCHAR(100);

UPDATE workout_session session
SET routine_name = routine.name
FROM routine
WHERE session.routine_id = routine.id;

ALTER TABLE workout_session DROP CONSTRAINT fk_workout_session_routine;
