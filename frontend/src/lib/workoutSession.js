export function workoutRequest(draft) {
  return {
    workoutDate: draft.workoutDate,
    routineId: draft.routineId,
    exercises: draft.exercises.map((exercise, index) => ({
      exerciseId: exercise.exerciseId,
      exerciseOrder: index + 1,
      sets: exercise.sets.map((set, setIndex) => ({
        setNumber: setIndex + 1,
        weight: Number(set.weight),
        reps: Number(set.reps),
        completed: Boolean(set.completed),
      })),
    })),
  }
}
