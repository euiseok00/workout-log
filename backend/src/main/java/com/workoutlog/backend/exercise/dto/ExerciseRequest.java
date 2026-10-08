package com.workoutlog.backend.exercise.dto;

import com.workoutlog.backend.exercise.BodyPart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(
		@NotBlank(message = "운동 이름은 공백일 수 없습니다.")
		@Size(max = 100, message = "운동 이름은 100자 이하여야 합니다.")
		String name,
		@NotNull(message = "운동 부위는 필수입니다.")
		BodyPart bodyPart) {
}
