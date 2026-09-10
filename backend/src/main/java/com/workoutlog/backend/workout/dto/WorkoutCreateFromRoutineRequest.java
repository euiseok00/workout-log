package com.workoutlog.backend.workout.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WorkoutCreateFromRoutineRequest(
	@NotNull
	@Positive
	Integer routineId,

	@NotNull
	LocalDate workoutDate,

	@Size(max = 50)
	String workoutTitle,

	String memo
) {
}
