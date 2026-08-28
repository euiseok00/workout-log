package com.workoutlog.backend.exercise.dto;

import com.workoutlog.backend.exercise.ExerciseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExerciseCreateRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@NotNull
	ExerciseCategory category
) {
}
