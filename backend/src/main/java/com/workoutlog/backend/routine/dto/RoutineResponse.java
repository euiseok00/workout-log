package com.workoutlog.backend.routine.dto;

import java.util.List;

import com.workoutlog.backend.routine.RoutineSummary;

public record RoutineResponse(
	Integer routineId,
	String routineName,
	String routineMemo,
	int exerciseCount,
	List<String> exercises
) {
	public static RoutineResponse from(RoutineSummary routine) {
		return new RoutineResponse(
			routine.id(),
			routine.name(),
			routine.memo(),
			routine.exerciseCount(),
			routine.exerciseNames()
		);
	}
}
