package com.workoutlog.backend.common.error;

public class RequestValidationException extends RuntimeException {

	private final String field;

	public RequestValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
