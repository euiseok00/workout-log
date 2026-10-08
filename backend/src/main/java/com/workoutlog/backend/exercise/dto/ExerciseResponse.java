package com.workoutlog.backend.exercise.dto;

import java.time.Instant;

import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.exercise.Exercise;

public record ExerciseResponse(
		Integer id,
		String name,
		BodyPart bodyPart,
		boolean custom,
		Instant createdAt) {

	public static ExerciseResponse from(Exercise exercise) {
		return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getBodyPart(),
				exercise.getUserId() != null, exercise.getCreatedAt());
	}
}
