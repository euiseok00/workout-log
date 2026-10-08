package com.workoutlog.backend.routine;

public class RoutineNotFoundException extends RuntimeException {

	public RoutineNotFoundException() {
		super("루틴을 찾을 수 없습니다.");
	}
}
