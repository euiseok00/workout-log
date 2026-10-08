package com.workoutlog.backend.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record WorkoutSessionRequest(
		@NotNull(message = "운동 날짜는 필수입니다.")
		@PastOrPresent(message = "미래 날짜는 사용할 수 없습니다.")
		LocalDate workoutDate,
		@Positive(message = "루틴 ID는 양수여야 합니다.")
		Integer routineId,
		@NotEmpty(message = "운동은 하나 이상이어야 합니다.")
		List<@NotNull(message = "운동 정보는 필수입니다.") @Valid ExerciseRequest> exercises) {

	public record ExerciseRequest(
			@NotNull(message = "운동 ID는 필수입니다.")
			@Positive(message = "운동 ID는 양수여야 합니다.")
			Integer exerciseId,
			@NotNull(message = "운동 순서는 필수입니다.")
			@Positive(message = "운동 순서는 양수여야 합니다.")
			Integer exerciseOrder,
			@NotNull(message = "세트 목록은 필수입니다.")
			List<@NotNull(message = "세트 정보는 필수입니다.") @Valid SetRequest> sets) {
	}

	public record SetRequest(
			@NotNull(message = "세트 번호는 필수입니다.")
			@Positive(message = "세트 번호는 1 이상이어야 합니다.")
			Integer setNumber,
			@NotNull(message = "중량은 필수입니다.")
			@DecimalMin(value = "0.00", message = "중량은 0 이상이어야 합니다.")
			@Digits(integer = 4, fraction = 2, message = "중량은 정수 4자리와 소수점 둘째 자리까지 입력할 수 있습니다.")
			BigDecimal weight,
			@NotNull(message = "반복 횟수는 필수입니다.")
			@PositiveOrZero(message = "반복 횟수는 0 이상이어야 합니다.")
			Integer reps,
			boolean completed) {
	}
}
