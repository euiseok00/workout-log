package com.workoutlog.backend.routine.dto;

import java.math.BigDecimal;
import java.util.List;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.routine.RoutineDetail;
import com.workoutlog.backend.routine.RoutineExerciseDetail;
import com.workoutlog.backend.routine.RoutineSetDetail;
import com.workoutlog.backend.routine.RoutineSetType;

public record RoutineDetailResponse(
	Integer routineId,
	String routineName,
	String routineMemo,
	List<RoutineExerciseResponse> exercises
) {
	public static RoutineDetailResponse from(RoutineDetail routine) {
		return new RoutineDetailResponse(
			routine.id(),
			routine.name(),
			routine.memo(),
			routine.exercises()
				.stream()
				.map(RoutineExerciseResponse::from)
				.toList()
		);
	}

	public record RoutineExerciseResponse(
		Integer exerciseId,
		String exerciseName,
		ExerciseCategory exerciseCategory,
		Integer exerciseOrder,
		String memo,
		List<RoutineSetResponse> sets
	) {
		static RoutineExerciseResponse from(RoutineExerciseDetail exercise) {
			return new RoutineExerciseResponse(
				exercise.exerciseId(),
				exercise.exerciseName(),
				exercise.exerciseCategory(),
				exercise.exerciseOrder(),
				exercise.memo(),
				exercise.sets()
					.stream()
					.map(RoutineSetResponse::from)
					.toList()
			);
		}
	}

	public record RoutineSetResponse(
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		RoutineSetType setType
	) {
		static RoutineSetResponse from(RoutineSetDetail set) {
			return new RoutineSetResponse(
				set.setOrder(),
				set.weight(),
				set.reps(),
				set.setType()
			);
		}
	}
}
