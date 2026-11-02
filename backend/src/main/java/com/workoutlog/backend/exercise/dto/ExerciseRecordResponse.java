package com.workoutlog.backend.exercise.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;
import com.workoutlog.backend.workout.WorkoutSession;

public record ExerciseRecordResponse(Integer sessionId, LocalDate workoutDate, Integer exerciseId,
		String exerciseName, List<ExerciseRecordSetResponse> sets) {

	public static ExerciseRecordResponse from(WorkoutSession session, ExerciseInSession exerciseInSession,
			Exercise exercise) {
		return new ExerciseRecordResponse(session.getId(), session.getWorkoutDate(), exercise.getId(),
				exercise.getName(), exerciseInSession.getSets().stream()
						.sorted(Comparator.comparing(ExerciseSet::getSetNumber))
						.map(ExerciseRecordSetResponse::from)
						.toList());
	}

	public record ExerciseRecordSetResponse(Integer setNumber, BigDecimal weight, Integer reps, boolean completed) {

		private static ExerciseRecordSetResponse from(ExerciseSet set) {
			return new ExerciseRecordSetResponse(set.getSetNumber(), set.getWeight(), set.getReps(), set.isCompleted());
		}
	}
}
