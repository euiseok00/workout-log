package com.workoutlog.backend.workout.controller;

import java.net.URI;

import com.workoutlog.backend.workout.WorkoutResponse;
import com.workoutlog.backend.workout.dto.WorkoutCreateFromRoutineRequest;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest;
import com.workoutlog.backend.workout.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/{workoutId}")
	public WorkoutResponse findWorkout(
		@PathVariable Integer workoutId
	) {
		return workoutService.findWorkout(workoutId);
	}
}
