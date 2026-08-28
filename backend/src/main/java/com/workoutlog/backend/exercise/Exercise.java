package com.workoutlog.backend.exercise;

public record Exercise(
	Integer id,
	String name,
	ExerciseType type,
	ExerciseCategory category,
	boolean active
) {
}
