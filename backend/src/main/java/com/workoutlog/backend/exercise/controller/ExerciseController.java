package com.workoutlog.backend.exercise.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.service.ExerciseService;
import com.workoutlog.backend.exercise.dto.ExerciseCreateRequest;
import com.workoutlog.backend.exercise.dto.ExerciseResponse;
import com.workoutlog.backend.exercise.dto.ExerciseUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
	private final ExerciseService exerciseService;

	public ExerciseController(ExerciseService exerciseService) {
		this.exerciseService = exerciseService;
	}

	@GetMapping
	public List<ExerciseResponse> findExercises(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam(required = false) ExerciseCategory category,
		@RequestParam(defaultValue = "ACTIVE") ExerciseStatus status
	) {
		return exerciseService.findExercises(userId(jwt), category, status.active())
			.stream()
			.map(ExerciseResponse::from)
			.toList();
	}

	@PostMapping
	public ResponseEntity<ExerciseResponse> createExercise(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody ExerciseCreateRequest request
	) {
		Exercise exercise = exerciseService.createCustomExercise(userId(jwt), request.name(), request.category());
		return ResponseEntity
			.created(URI.create("/api/exercises/" + exercise.id()))
			.body(ExerciseResponse.from(exercise));
	}

	@PutMapping("/{exerciseId}")
	public ExerciseResponse updateExercise(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer exerciseId,
		@Valid @RequestBody ExerciseUpdateRequest request
	) {
		Exercise exercise = exerciseService.updateCustomExercise(
			userId(jwt),
			exerciseId,
			request.name(),
			request.category()
		);
		return ExerciseResponse.from(exercise);
	}

	@PatchMapping("/{exerciseId}/inactive")
	public ResponseEntity<Void> deactivateExercise(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer exerciseId
	) {
		exerciseService.deactivateCustomExercise(userId(jwt), exerciseId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{exerciseId}/active")
	public ResponseEntity<Void> activateExercise(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer exerciseId
	) {
		exerciseService.activateCustomExercise(userId(jwt), exerciseId);
		return ResponseEntity.noContent().build();
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

	private enum ExerciseStatus {
		ACTIVE,
		INACTIVE,
		ALL;

		private Boolean active() {
			return switch (this) {
				case ACTIVE -> true;
				case INACTIVE -> false;
				case ALL -> null;
			};
		}
	}
}
