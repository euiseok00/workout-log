// 루틴이 삭제된 기록은 routineId가 null이므로 자유 운동으로 시작한다.
export function draftFromSession(session, workoutDate) {
  return {
    workoutDate,
    routineId: session.routineId,
    routineName: session.routineId == null ? null : session.routineName,
    exercises: session.exercises.map((exercise, exerciseIndex) => ({
      ...exercise,
      exerciseOrder: exerciseIndex + 1,
      sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1, completed: false })),
    })),
    updatedAt: new Date().toISOString(),
  }
}

export function workoutRequest(draft) {
  return {
    workoutDate: draft.workoutDate,
    routineId: draft.routineId,
    exercises: draft.exercises.map((exercise, index) => ({
      exerciseId: exercise.exerciseId,
      exerciseOrder: index + 1,
      groupNumber: exercise.groupNumber ?? null,
      sets: exercise.sets.map((set, setIndex) => ({
        setNumber: setIndex + 1,
        weight: Number(set.weight),
        reps: Number(set.reps),
        completed: Boolean(set.completed),
        setType: set.setType || 'WORKING',
      })),
    })),
  }
}
