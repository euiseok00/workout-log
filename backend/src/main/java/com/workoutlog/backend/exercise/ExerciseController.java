package com.workoutlog.backend.exercise;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workoutlog.backend.exercise.dto.ExerciseRequest;
import com.workoutlog.backend.exercise.dto.ExerciseRecordResponse;
import com.workoutlog.backend.exercise.dto.ExerciseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

	private final ExerciseService exerciseService;

	public ExerciseController(ExerciseService exerciseService) {
		this.exerciseService = exerciseService;
	}

	@GetMapping
	public List<ExerciseResponse> getExercises(@AuthenticationPrincipal Jwt jwt) {
		return exerciseService.getExercises(Integer.valueOf(jwt.getSubject()));
	}

	@GetMapping("/{exerciseId}/records")
	public List<ExerciseRecordResponse> getExerciseRecords(@PathVariable Integer exerciseId,
			@AuthenticationPrincipal Jwt jwt) {
		return exerciseService.getExerciseRecords(exerciseId, Integer.valueOf(jwt.getSubject()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ExerciseResponse createExercise(@Valid @RequestBody ExerciseRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return exerciseService.createExercise(request, Integer.valueOf(jwt.getSubject()));
	}
}
