package com.workoutlog.backend.workout;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workoutlog.backend.workout.dto.WorkoutSessionRequest;
import com.workoutlog.backend.workout.dto.WorkoutSessionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workout-sessions")
public class WorkoutSessionController {

	private final WorkoutSessionService workoutSessionService;

	public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
		this.workoutSessionService = workoutSessionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WorkoutSessionResponse createSession(@Valid @RequestBody WorkoutSessionRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return workoutSessionService.createSession(request, Integer.valueOf(jwt.getSubject()));
	}

	@GetMapping
	public List<WorkoutSessionResponse> getSessions(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@AuthenticationPrincipal Jwt jwt) {
		return workoutSessionService.getSessions(Integer.valueOf(jwt.getSubject()), from, to);
	}

	@GetMapping("/{sessionId}")
	public WorkoutSessionResponse getSession(@PathVariable Integer sessionId, @AuthenticationPrincipal Jwt jwt) {
		return workoutSessionService.getSession(sessionId, Integer.valueOf(jwt.getSubject()));
	}

	@PutMapping("/{sessionId}")
	public WorkoutSessionResponse updateSession(@PathVariable Integer sessionId,
			@Valid @RequestBody WorkoutSessionRequest request, @AuthenticationPrincipal Jwt jwt) {
		return workoutSessionService.updateSession(sessionId, request, Integer.valueOf(jwt.getSubject()));
	}
}
