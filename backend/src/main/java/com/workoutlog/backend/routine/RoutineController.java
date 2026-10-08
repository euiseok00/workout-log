package com.workoutlog.backend.routine;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workoutlog.backend.routine.dto.RoutineRequest;
import com.workoutlog.backend.routine.dto.RoutineResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {

	private final RoutineService routineService;

	public RoutineController(RoutineService routineService) {
		this.routineService = routineService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoutineResponse createRoutine(@Valid @RequestBody RoutineRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return routineService.createRoutine(request, Integer.valueOf(jwt.getSubject()));
	}

	@GetMapping
	public List<RoutineResponse> getRoutines(@AuthenticationPrincipal Jwt jwt) {
		return routineService.getRoutines(Integer.valueOf(jwt.getSubject()));
	}

	@GetMapping("/{routineId}")
	public RoutineResponse getRoutine(@PathVariable Integer routineId, @AuthenticationPrincipal Jwt jwt) {
		return routineService.getRoutine(routineId, Integer.valueOf(jwt.getSubject()));
	}

	@PutMapping("/{routineId}")
	public RoutineResponse updateRoutine(@PathVariable Integer routineId, @Valid @RequestBody RoutineRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return routineService.updateRoutine(routineId, request, Integer.valueOf(jwt.getSubject()));
	}
}
