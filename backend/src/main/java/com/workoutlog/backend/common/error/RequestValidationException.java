package com.workoutlog.backend.common.error;

import java.util.Collection;
import java.util.HashSet;

public class RequestValidationException extends RuntimeException {

	private final String field;

	public RequestValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public static void requireDistinct(Collection<Integer> values, String field, String message) {
		if (new HashSet<>(values).size() != values.size()) {
			throw new RequestValidationException(field, message);
		}
	}

	public String getField() {
		return field;
	}
}
