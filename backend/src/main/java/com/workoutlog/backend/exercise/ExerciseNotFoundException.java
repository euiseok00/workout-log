package com.workoutlog.backend.exercise;

import org.springframework.http.HttpStatus;

import com.workoutlog.backend.common.error.ApiException;

public class ExerciseNotFoundException extends ApiException {

	public ExerciseNotFoundException() {
		super(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "운동을 찾을 수 없습니다.");
	}
}
