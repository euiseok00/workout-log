package com.workoutlog.backend.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.workoutlog.backend.workout.WorkoutSetType;

public record ExerciseStatisticsDateResponse(
	LocalDate date,
	BigDecimal volume,
	int workoutCount,
	List<WorkoutResponse> workouts
) {
	public record WorkoutResponse(
		Integer workoutId,
		Integer workoutOrder,
		List<SetResponse> sets
	) {
	}

	public record SetResponse(
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		WorkoutSetType setType,
		boolean completed
	) {
	}
}
