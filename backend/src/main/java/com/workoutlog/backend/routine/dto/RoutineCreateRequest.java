package com.workoutlog.backend.routine.dto;

import java.math.BigDecimal;
import java.util.List;

import com.workoutlog.backend.routine.RoutineSetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RoutineCreateRequest(
	@NotBlank
	@Size(max = 50)
	String routineName,

	String routineMemo,

	@NotEmpty
	@Valid
	List<RoutineExerciseRequest> exercises
) {
	public record RoutineExerciseRequest(
		@NotNull
		@Positive
		Integer exerciseId,

		@NotNull
		@Positive
		Integer exerciseOrder,

		String memo,

		@NotEmpty
		@Valid
		List<RoutineSetRequest> sets
	) {
	}

	public record RoutineSetRequest(
		@NotNull
		@Positive
		Integer setOrder,

		@NotNull
		@DecimalMin("0.0")
		@Digits(integer = 3, fraction = 2)
		BigDecimal weight,

		@NotNull
		@PositiveOrZero
		Integer reps,

		@NotNull
		RoutineSetType setType
	) {
	}
}
