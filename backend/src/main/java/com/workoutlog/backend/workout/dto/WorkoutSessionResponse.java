package com.workoutlog.backend.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;
import com.workoutlog.backend.workout.WorkoutSession;

public record WorkoutSessionResponse(
		Integer id,
		LocalDate workoutDate,
		Integer routineId,
		List<ExerciseResponse> exercises) {

	public static WorkoutSessionResponse from(WorkoutSession session) {
		return new WorkoutSessionResponse(session.getId(), session.getWorkoutDate(), session.getRoutineId(),
				session.getExercises().stream()
						.sorted(Comparator.comparing(ExerciseInSession::getExerciseOrder))
						.map(ExerciseResponse::from)
						.toList());
	}

	public record ExerciseResponse(
			Integer exerciseId,
			String name,
			BodyPart bodyPart,
			Integer exerciseOrder,
			List<SetResponse> sets) {

		static ExerciseResponse from(ExerciseInSession exercise) {
			return new ExerciseResponse(exercise.getExercise().getId(), exercise.getExercise().getName(),
					exercise.getExercise().getBodyPart(), exercise.getExerciseOrder(),
					exercise.getSets().stream()
							.sorted(Comparator.comparing(ExerciseSet::getSetNumber))
							.map(SetResponse::from)
							.toList());
		}
	}

	public record SetResponse(
			Integer setNumber,
			BigDecimal weight,
			Integer reps,
			boolean completed) {

		static SetResponse from(ExerciseSet set) {
			return new SetResponse(set.getSetNumber(), set.getWeight(), set.getReps(), set.isCompleted());
		}
	}
}
