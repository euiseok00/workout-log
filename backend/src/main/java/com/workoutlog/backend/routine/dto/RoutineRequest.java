package com.workoutlog.backend.routine.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoutineRequest(
		@NotBlank(message = "루틴 이름은 공백일 수 없습니다.")
		@Size(max = 100, message = "루틴 이름은 100자 이하여야 합니다.")
		String name,
		@NotEmpty(message = "운동은 하나 이상이어야 합니다.")
		List<@NotNull(message = "운동 정보는 필수입니다.") @Valid ExerciseRequest> exercises) {

	public record ExerciseRequest(
			@NotNull(message = "운동 ID는 필수입니다.")
			@Positive(message = "운동 ID는 양수여야 합니다.")
			Integer exerciseId,
			@NotNull(message = "운동 순서는 필수입니다.")
			@Positive(message = "운동 순서는 양수여야 합니다.")
			Integer exerciseOrder,
			@Positive(message = "목표 세트 수는 양수여야 합니다.")
			Integer targetSets,
			@Positive(message = "목표 반복 수는 양수여야 합니다.")
			Integer targetReps) {
	}
}
