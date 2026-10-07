package com.workoutlog.backend.common.error;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요.", "VALIDATION_FAILED",
				request, errors);
		problem.setTitle("Validation failed");
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
		List<FieldErrorResponse> errors = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldErrorResponse(result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요.", "VALIDATION_FAILED",
				request, errors);
		problem.setTitle("Validation failed");
		return problem;
	}

	@ExceptionHandler(RequestValidationException.class)
	ProblemDetail handleRequestValidation(RequestValidationException exception, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "요청 값을 확인해 주세요.", "VALIDATION_FAILED", request,
				List.of(new FieldErrorResponse(exception.getField(), exception.getMessage())));
		problem.setTitle("Validation failed");
		return problem;
	}

	@ExceptionHandler(DuplicateLoginIdException.class)
	ProblemDetail handleDuplicateLoginId(DuplicateLoginIdException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, exception.getMessage(), "LOGIN_ID_ALREADY_EXISTS", request, null);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), "INVALID_CREDENTIALS", request, null);
	}

	private ProblemDetail problem(HttpStatus status, String detail, String code, HttpServletRequest request,
			List<FieldErrorResponse> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		if (errors != null) {
			problem.setProperty("errors", errors);
		}
		return problem;
	}

	private record FieldErrorResponse(String field, String message) {
	}
}
