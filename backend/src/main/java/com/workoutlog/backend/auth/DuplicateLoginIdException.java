package com.workoutlog.backend.auth;

import org.springframework.http.HttpStatus;

import com.workoutlog.backend.common.error.ApiException;

public class DuplicateLoginIdException extends ApiException {

	public DuplicateLoginIdException() {
		super(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.");
	}
}
