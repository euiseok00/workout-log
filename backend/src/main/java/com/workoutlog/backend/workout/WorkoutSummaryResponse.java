package com.workoutlog.backend.workout;

import java.time.LocalDate;

public record WorkoutSummaryResponse(
	Integer workoutId,
	LocalDate workoutDate,
	Integer workoutOrder,
	String memo,
	Integer exerciseCount,
	Integer setCount
) {
}
