package com.workoutlog.backend.auth;

import org.springframework.http.HttpStatus;

import com.workoutlog.backend.common.error.ApiException;

public class InvalidCredentialsException extends ApiException {

	public InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
	}
}
