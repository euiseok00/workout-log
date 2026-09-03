-- Workout Log Supabase PostgreSQL schema
-- Apply to an empty Supabase project/database.

-- Reset helper for local/dev use only. Uncomment deliberately if a full reset is needed.
-- DROP TABLE IF EXISTS public.workout_sets;
-- DROP TABLE IF EXISTS public.workout_exercises;
-- DROP TABLE IF EXISTS public.workouts;
-- DROP TABLE IF EXISTS public.routine_sets;
-- DROP TABLE IF EXISTS public.routine_exercises;
-- DROP TABLE IF EXISTS public.routines;
-- DROP TABLE IF EXISTS public.exercises;

CREATE TABLE public.exercises (
    exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_name VARCHAR(50) NOT NULL,
    exercise_type VARCHAR(10) NOT NULL,
    exercise_category VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CHECK (exercise_type IN ('SYSTEM', 'CUSTOM')),
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
    ),
    CHECK (
        (exercise_type = 'SYSTEM' AND user_id IS NULL)
        OR
        (exercise_type = 'CUSTOM' AND user_id IS NOT NULL)
    )
);

CREATE TABLE public.routines (
    routine_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    routine_name VARCHAR(50) NOT NULL,
    routine_memo TEXT
);

CREATE TABLE public.routine_exercises (
    routine_exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_id INTEGER NOT NULL REFERENCES public.routines(routine_id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES public.exercises(exercise_id),
    exercise_order INTEGER NOT NULL,
    memo TEXT,

    UNIQUE (routine_id, exercise_order),
    CHECK (exercise_order >= 1)
);

CREATE TABLE public.routine_sets (
    routine_set_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_exercise_id INTEGER NOT NULL REFERENCES public.routine_exercises(routine_exercise_id) ON DELETE CASCADE,
    set_order INTEGER NOT NULL,
    weight NUMERIC(5,2) NOT NULL,
    reps INTEGER NOT NULL,
    set_type VARCHAR(10) NOT NULL,

    UNIQUE (routine_exercise_id, set_order),
    CHECK (set_order >= 1),
    CHECK (weight >= 0),
    CHECK (reps >= 0),
    CHECK (
        set_type IN (
            'WARMUP',
            'WORKING',
            'TOP',
            'FAILURE',
            'BACKOFF',
            'DROP'
        )
    )
);

CREATE TABLE public.workouts (
    workout_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_date DATE NOT NULL,
    workout_order INTEGER NOT NULL,
    memo TEXT,

    UNIQUE (user_id, workout_date, workout_order),
    CHECK (workout_order >= 1)
);

CREATE TABLE public.workout_exercises (
    workout_exercise_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_id INTEGER NOT NULL REFERENCES public.workouts(workout_id) ON DELETE CASCADE,
    exercise_id INTEGER NOT NULL REFERENCES public.exercises(exercise_id),
    exercise_name VARCHAR(50) NOT NULL,
    exercise_category VARCHAR(10) NOT NULL,
    exercise_order INTEGER NOT NULL,
    memo TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,

    UNIQUE (workout_id, exercise_order),
    CHECK (exercise_order >= 1),
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

CREATE TABLE public.workout_sets (
    workout_set_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_exercise_id INTEGER NOT NULL REFERENCES public.workout_exercises(workout_exercise_id) ON DELETE CASCADE,
    set_order INTEGER NOT NULL,
    weight NUMERIC(5,2) NOT NULL,
    reps INTEGER NOT NULL,
    rpe INTEGER,
    set_type VARCHAR(10) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,

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
            'FAILURE',
            'BACKOFF',
            'DROP'
        )
    )
);

CREATE INDEX idx_exercises_user_id ON public.exercises(user_id);
CREATE INDEX idx_routines_user_id ON public.routines(user_id);
CREATE INDEX idx_workouts_user_id ON public.workouts(user_id);

ALTER TABLE public.exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.routines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.routine_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.routine_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_sets ENABLE ROW LEVEL SECURITY;
