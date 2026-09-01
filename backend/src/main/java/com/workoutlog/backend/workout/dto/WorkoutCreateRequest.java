package com.workoutlog.backend.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.workoutlog.backend.workout.WorkoutSetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record WorkoutCreateRequest(
	@NotNull
	LocalDate workoutDate,

	String memo,

	@NotEmpty
	@Valid
	List<WorkoutExerciseRequest> exercises
) {
	public record WorkoutExerciseRequest(
		@NotNull
		@Positive
		Integer exerciseId,

		@NotNull
		@Positive
		Integer exerciseOrder,

		String memo,

		Boolean completed,

		@NotEmpty
		@Valid
		List<WorkoutSetRequest> sets
	) {
	}

	public record WorkoutSetRequest(
		@NotNull
		@Positive
		Integer setOrder,

		@NotNull
		@DecimalMin("0.0")
		BigDecimal weight,

		@NotNull
		@PositiveOrZero
		Integer reps,

		@Min(1)
		@Max(10)
		Integer rpe,

		@NotNull
		WorkoutSetType setType,

		Boolean completed
	) {
	}
}
