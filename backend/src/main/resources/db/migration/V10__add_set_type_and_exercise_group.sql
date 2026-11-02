-- 기존 세트는 워킹 세트로 간주한다. 묶음 번호는 같은 세션/루틴 안에서 함께 수행하는 운동을 나타낸다.
ALTER TABLE exercise_set
    ADD COLUMN set_type VARCHAR(20) NOT NULL DEFAULT 'WORKING',
    ADD CONSTRAINT ck_exercise_set_type CHECK (set_type IN
        ('WORKING', 'WARMUP', 'SUPER', 'TRI', 'GIANT', 'DROP', 'TOP', 'FAILURE', 'ASSIST', 'BACKOFF'));

ALTER TABLE routine_set
    ADD COLUMN set_type VARCHAR(20) NOT NULL DEFAULT 'WORKING',
    ADD CONSTRAINT ck_routine_set_type CHECK (set_type IN
        ('WORKING', 'WARMUP', 'SUPER', 'TRI', 'GIANT', 'DROP', 'TOP', 'FAILURE', 'ASSIST', 'BACKOFF'));

ALTER TABLE exercise_in_session
    ADD COLUMN group_number INTEGER NULL,
    ADD CONSTRAINT ck_exercise_in_session_group CHECK (group_number >= 1);

ALTER TABLE exercise_in_routine
    ADD COLUMN group_number INTEGER NULL,
    ADD CONSTRAINT ck_exercise_in_routine_group CHECK (group_number >= 1);

CREATE INDEX idx_workout_session_routine ON workout_session (routine_id);
