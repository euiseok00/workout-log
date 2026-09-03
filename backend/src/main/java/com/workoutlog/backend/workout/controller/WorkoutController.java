package com.workoutlog.backend.workout.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.workout.WorkoutResponse;
import com.workoutlog.backend.workout.WorkoutSummaryResponse;
import com.workoutlog.backend.workout.dto.WorkoutCreateFromRoutineRequest;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest;
import com.workoutlog.backend.workout.service.WorkoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {
	private final WorkoutService workoutService;

	public WorkoutController(WorkoutService workoutService) {
		this.workoutService = workoutService;
	}

	@PostMapping
	public ResponseEntity<WorkoutResponse> createWorkout(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody WorkoutCreateRequest request
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		WorkoutResponse workout = workoutService.createWorkout(
			userId,
			request.workoutDate(),
			request.memo(),
			request.exercises()
		);

		return ResponseEntity
			.created(URI.create("/api/workouts/" + workout.workoutId()))
			.body(workout);
	}

	@PostMapping("/from-routine")
	public ResponseEntity<WorkoutResponse> createWorkoutFromRoutine(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody WorkoutCreateFromRoutineRequest request
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		WorkoutResponse workout = workoutService.createWorkoutFromRoutine(
			userId,
			request.routineId(),
			request.workoutDate(),
			request.memo()
		);

		return ResponseEntity
			.created(URI.create("/api/workouts/" + workout.workoutId()))
			.body(workout);
	}

	@GetMapping("/calendar")
	public List<LocalDate> findWorkoutCalendarDates(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam @NotNull Integer year,
		@RequestParam @NotNull @Min(1) @Max(12) Integer month
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return workoutService.findWorkoutDates(userId, year, month);
	}

	@GetMapping
	public List<WorkoutSummaryResponse> findWorkoutsByDate(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return workoutService.findWorkoutSummariesByDate(userId, date);
	}

	@GetMapping("/{workoutId}")
	public WorkoutResponse findWorkout(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable Integer workoutId
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return workoutService.findWorkout(userId, workoutId);
	}

	@PutMapping("/{workoutId}")
	public WorkoutResponse updateWorkout(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer workoutId,
		@Valid @RequestBody WorkoutCreateRequest request
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return workoutService.updateWorkout(
			userId,
			workoutId,
			request.workoutDate(),
			request.memo(),
			request.exercises()
		);
	}

	@DeleteMapping("/{workoutId}")
	public ResponseEntity<Void> deleteWorkout(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer workoutId
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		workoutService.deleteWorkout(userId, workoutId);
		return ResponseEntity.noContent().build();
	}
}
