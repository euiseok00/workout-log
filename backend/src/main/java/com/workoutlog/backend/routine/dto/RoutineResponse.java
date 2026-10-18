package com.workoutlog.backend.routine.dto;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.routine.ExerciseInRoutine;
import com.workoutlog.backend.routine.Routine;
import com.workoutlog.backend.routine.RoutineSet;

public record RoutineResponse(
		Integer id,
		String name,
		List<ExerciseResponse> exercises,
		Instant createdAt,
		Instant updatedAt) {

	public static RoutineResponse from(Routine routine) {
		return new RoutineResponse(routine.getId(), routine.getName(),
				routine.getExercises().stream()
						.sorted(Comparator.comparing(ExerciseInRoutine::getExerciseOrder))
						.map(ExerciseResponse::from)
						.toList(),
				routine.getCreatedAt(), routine.getUpdatedAt());
	}

	public record ExerciseResponse(
			Integer exerciseId,
			String name,
			BodyPart bodyPart,
			Integer exerciseOrder,
			Integer targetSets,
			Integer targetReps,
			List<SetResponse> sets) {

		static ExerciseResponse from(ExerciseInRoutine configuredExercise) {
			return new ExerciseResponse(configuredExercise.getExercise().getId(),
					configuredExercise.getExercise().getName(), configuredExercise.getExercise().getBodyPart(),
					configuredExercise.getExerciseOrder(), configuredExercise.getTargetSets(),
					configuredExercise.getTargetReps(), configuredExercise.getSets().stream()
							.sorted(Comparator.comparing(RoutineSet::getSetNumber))
							.map(set -> new SetResponse(set.getSetNumber(), set.getWeight(), set.getReps()))
							.toList());
		}
	}

	public record SetResponse(Integer setNumber, BigDecimal weight, Integer reps) {
	}
}
