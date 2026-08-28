package com.workoutlog.backend.exercise.dto;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseType;

public record ExerciseResponse(
	Integer id,
	String name,
	ExerciseType type,
	ExerciseCategory category,
	boolean active
) {
	public static ExerciseResponse from(Exercise exercise) {
		return new ExerciseResponse(
			exercise.id(),
			exercise.name(),
			exercise.type(),
			exercise.category(),
			exercise.active()
		);
	}
}
