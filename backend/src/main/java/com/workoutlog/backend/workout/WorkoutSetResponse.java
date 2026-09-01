package com.workoutlog.backend.workout;

import java.math.BigDecimal;

public record WorkoutSetResponse(
	Integer setOrder,
	BigDecimal weight,
	Integer reps,
	Integer rpe,
	WorkoutSetType setType,
	boolean completed
) {
}
