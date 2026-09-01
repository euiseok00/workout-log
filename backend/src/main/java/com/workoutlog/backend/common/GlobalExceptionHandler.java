package com.workoutlog.backend.common;

import java.util.stream.Collectors;

import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseOperationException;
import com.workoutlog.backend.routine.RoutineNotFoundException;
import com.workoutlog.backend.routine.RoutineOperationException;
import com.workoutlog.backend.workout.WorkoutNotFoundException;
import com.workoutlog.backend.workout.WorkoutOperationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(ExerciseNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ExerciseNotFoundException exception) {
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(new ApiErrorResponse("EXERCISE_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(RoutineNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleRoutineNotFound(RoutineNotFoundException exception) {
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(new ApiErrorResponse("ROUTINE_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(WorkoutNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleWorkoutNotFound(WorkoutNotFoundException exception) {
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(new ApiErrorResponse("WORKOUT_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(ExerciseOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleExerciseOperation(ExerciseOperationException exception) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ApiErrorResponse("INVALID_EXERCISE_OPERATION", exception.getMessage()));
	}

	@ExceptionHandler(RoutineOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleRoutineOperation(RoutineOperationException exception) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ApiErrorResponse("INVALID_ROUTINE_OPERATION", exception.getMessage()));
	}

	@ExceptionHandler(WorkoutOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleWorkoutOperation(WorkoutOperationException exception) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ApiErrorResponse("INVALID_WORKOUT_OPERATION", exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + " " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		return ResponseEntity
			.badRequest()
			.body(new ApiErrorResponse("VALIDATION_ERROR", message));
	}

	@ExceptionHandler({
		ConstraintViolationException.class,
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
		return ResponseEntity
			.badRequest()
			.body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage()));
	}
}
