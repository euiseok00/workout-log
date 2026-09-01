package com.workoutlog.backend.workout;

import java.util.List;

public record WorkoutExerciseResponse(
	Integer exerciseId,
	String exerciseName,
	Integer exerciseOrder,
	String memo,
	boolean completed,
	List<WorkoutSetResponse> sets
) {
}
