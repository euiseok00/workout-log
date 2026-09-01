package com.workoutlog.backend.workout.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WorkoutCreateFromRoutineRequest(
	@NotNull
	@Positive
	Integer routineId,

	@NotNull
	LocalDate workoutDate,

	String memo
) {
}
