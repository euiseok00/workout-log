package com.workoutlog.backend.exercise;

public class ExerciseNotFoundException extends RuntimeException {

	public ExerciseNotFoundException() {
		super("운동을 찾을 수 없습니다.");
	}
}
