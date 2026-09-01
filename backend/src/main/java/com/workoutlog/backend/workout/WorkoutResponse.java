package com.workoutlog.backend.workout;

import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(
	Integer workoutId,
	LocalDate workoutDate,
	Integer workoutOrder,
	String memo,
	List<WorkoutExerciseResponse> exercises
) {
}
