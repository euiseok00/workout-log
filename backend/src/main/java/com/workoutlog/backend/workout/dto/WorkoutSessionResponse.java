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
		String routineName,
		List<SessionExerciseResponse> exercises) {

	public static WorkoutSessionResponse from(WorkoutSession session, String routineName) {
		return new WorkoutSessionResponse(session.getId(), session.getWorkoutDate(), session.getRoutineId(), routineName,
				session.getExercises().stream()
						.sorted(Comparator.comparing(ExerciseInSession::getExerciseOrder))
						.map(SessionExerciseResponse::from)
						.toList());
	}

	public record SessionExerciseResponse(
			Integer exerciseId,
			String name,
			BodyPart bodyPart,
			Integer exerciseOrder,
			List<SessionSetResponse> sets) {

		static SessionExerciseResponse from(ExerciseInSession exercise) {
			return new SessionExerciseResponse(exercise.getExercise().getId(), exercise.getExercise().getName(),
					exercise.getExercise().getBodyPart(), exercise.getExerciseOrder(),
					exercise.getSets().stream()
							.sorted(Comparator.comparing(ExerciseSet::getSetNumber))
							.map(SessionSetResponse::from)
							.toList());
		}
	}

	public record SessionSetResponse(
			Integer setNumber,
			BigDecimal weight,
			Integer reps,
			boolean completed) {

		static SessionSetResponse from(ExerciseSet set) {
			return new SessionSetResponse(set.getSetNumber(), set.getWeight(), set.getReps(), set.isCompleted());
		}
	}
}
