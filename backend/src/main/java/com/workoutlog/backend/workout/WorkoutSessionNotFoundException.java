package com.workoutlog.backend.workout;

public class WorkoutSessionNotFoundException extends RuntimeException {

	public WorkoutSessionNotFoundException() {
		super("운동 기록을 찾을 수 없습니다.");
	}
}
