package com.workoutlog.backend.workout.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
		@Valid @RequestBody WorkoutCreateRequest request
	) {
		WorkoutResponse workout = workoutService.createWorkout(
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
		@Valid @RequestBody WorkoutCreateFromRoutineRequest request
	) {
		WorkoutResponse workout = workoutService.createWorkoutFromRoutine(
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
		@RequestParam @NotNull Integer year,
		@RequestParam @NotNull @Min(1) @Max(12) Integer month
	) {
		return workoutService.findWorkoutDates(year, month);
	}

	@GetMapping
	public List<WorkoutSummaryResponse> findWorkoutsByDate(
		@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return workoutService.findWorkoutSummariesByDate(date);
	}

	@GetMapping("/{workoutId}")
	public WorkoutResponse findWorkout(
		@PathVariable Integer workoutId
	) {
		return workoutService.findWorkout(workoutId);
	}

	@DeleteMapping("/{workoutId}")
	public ResponseEntity<Void> deleteWorkout(
		@PathVariable @Positive Integer workoutId
	) {
		workoutService.deleteWorkout(workoutId);
		return ResponseEntity.noContent().build();
	}
}
