package com.workoutlog.backend.exercise;

public class ExerciseNotFoundException extends RuntimeException {
	public ExerciseNotFoundException(Integer exerciseId) {
		super("Exercise not found: " + exerciseId);
	}
}
