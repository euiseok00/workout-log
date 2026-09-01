-- =========================================
-- 1. 운동 종목
-- =========================================

CREATE TABLE exercises (
    exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_name VARCHAR(50) NOT NULL,
    exercise_type VARCHAR(10) NOT NULL,
    exercise_category VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CHECK (
        exercise_type IN ('SYSTEM', 'CUSTOM')
    ),

    CHECK (
        exercise_category IN (
            'CHEST',
            'BACK',
            'LEGS',
            'SHOULDER',
            'BICEPS',
            'TRICEPS',
            'CARDIO',
            'ETC'
        )
    )
);


-- =========================================
-- 2. 루틴
-- =========================================

CREATE TABLE routines (
    routine_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_name VARCHAR(50) NOT NULL,
    routine_memo TEXT
);


-- =========================================
-- 3. 루틴 운동
-- =========================================

CREATE TABLE routine_exercises (
    routine_exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    exercise_order INTEGER NOT NULL,
    memo TEXT,

    FOREIGN KEY (routine_id)
        REFERENCES routines(routine_id)
        ON DELETE CASCADE,

    FOREIGN KEY (exercise_id)
        REFERENCES exercises(exercise_id),

    UNIQUE (routine_id, exercise_order),

    CHECK (exercise_order >= 1)
);


-- =========================================
-- 4. 루틴 세트
-- =========================================

CREATE TABLE routine_sets (
    routine_set_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_exercise_id INTEGER NOT NULL,
    set_order INTEGER NOT NULL,
    weight NUMERIC(5,2) NOT NULL,
    reps INTEGER NOT NULL,
    set_type VARCHAR(10) NOT NULL,

    FOREIGN KEY (routine_exercise_id)
        REFERENCES routine_exercises(routine_exercise_id)
        ON DELETE CASCADE,

    UNIQUE (routine_exercise_id, set_order),

    CHECK (set_order >= 1),
    CHECK (weight >= 0),
    CHECK (reps >= 0),

    CHECK (
        set_type IN (
            'WARMUP',
            'WORKING',
            'TOP',
            'FAILURE'
        )
    )
);


-- =========================================
-- 5. 날짜별 운동 세션
-- =========================================

CREATE TABLE workouts (
    workout_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_date DATE NOT NULL,
    workout_order INTEGER NOT NULL,
    memo TEXT,

    UNIQUE (workout_date, workout_order),

    CHECK (workout_order >= 1)
);


-- =========================================
-- 6. 실제 수행 운동
-- =========================================

CREATE TABLE workout_exercises (
    workout_exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,

    -- 당시 운동명을 보존하기 위한 스냅샷
    exercise_name VARCHAR(50) NOT NULL,

    exercise_order INTEGER NOT NULL,
    memo TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (workout_id)
        REFERENCES workouts(workout_id)
        ON DELETE CASCADE,

    FOREIGN KEY (exercise_id)
        REFERENCES exercises(exercise_id),

    UNIQUE (workout_id, exercise_order),

    CHECK (exercise_order >= 1)
);


-- =========================================
-- 7. 실제 수행 세트
-- =========================================

CREATE TABLE workout_sets (
    workout_set_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_exercise_id INTEGER NOT NULL,
    set_order INTEGER NOT NULL,
    weight NUMERIC(5,2) NOT NULL,
    reps INTEGER NOT NULL,
    rpe INTEGER,
    set_type VARCHAR(10) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (workout_exercise_id)
        REFERENCES workout_exercises(workout_exercise_id)
        ON DELETE CASCADE,

    UNIQUE (workout_exercise_id, set_order),

    CHECK (set_order >= 1),
    CHECK (weight >= 0),
    CHECK (reps >= 0),
    CHECK (rpe IS NULL OR (rpe >= 1 AND rpe <= 10)),

    CHECK (
        set_type IN (
            'WARMUP',
            'WORKING',
            'TOP',
            'FAILURE'
        )
    )
);