package com.workoutlog.backend.common;

public record ApiErrorResponse(
	String code,
	String message
) {
}
