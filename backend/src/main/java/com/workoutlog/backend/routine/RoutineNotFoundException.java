package com.workoutlog.backend.routine;

public class RoutineNotFoundException extends RuntimeException {
	public RoutineNotFoundException(Integer routineId) {
		super("Routine not found: " + routineId);
	}
}
