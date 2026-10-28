-- 삭제된 루틴을 가리키는 routine_id를 정리한 뒤 FK를 복원한다. 루틴 삭제 시 기록은 routine_name만 남긴다.
UPDATE workout_session
SET routine_id = NULL
WHERE routine_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM routine WHERE routine.id = workout_session.routine_id);

ALTER TABLE workout_session
    ADD CONSTRAINT fk_workout_session_routine
        FOREIGN KEY (routine_id) REFERENCES routine (id) ON DELETE SET NULL;
