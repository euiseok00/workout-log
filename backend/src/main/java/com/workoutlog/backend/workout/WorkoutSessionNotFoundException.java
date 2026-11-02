package com.workoutlog.backend.workout;

import org.springframework.http.HttpStatus;

import com.workoutlog.backend.common.error.ApiException;

public class WorkoutSessionNotFoundException extends ApiException {

	public WorkoutSessionNotFoundException() {
		super(HttpStatus.NOT_FOUND, "WORKOUT_SESSION_NOT_FOUND", "운동 기록을 찾을 수 없습니다.");
	}
}
