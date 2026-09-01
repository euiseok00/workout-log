package com.workoutlog.backend.workout;

public class WorkoutNotFoundException extends RuntimeException {
	public WorkoutNotFoundException(Integer workoutId) {
		super("Workout not found. workoutId=" + workoutId);
	}
}
