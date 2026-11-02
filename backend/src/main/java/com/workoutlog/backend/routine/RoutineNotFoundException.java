package com.workoutlog.backend.routine;

import org.springframework.http.HttpStatus;

import com.workoutlog.backend.common.error.ApiException;

public class RoutineNotFoundException extends ApiException {

	public RoutineNotFoundException() {
		super(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "루틴을 찾을 수 없습니다.");
	}
}
