package com.workoutlog.backend.routine.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.routine.RoutineSummary;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest;
import com.workoutlog.backend.routine.dto.RoutineDetailResponse;
import com.workoutlog.backend.routine.dto.RoutineResponse;
import com.workoutlog.backend.routine.service.RoutineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/routines")
public class RoutineController {
	private final RoutineService routineService;

	public RoutineController(RoutineService routineService) {
		this.routineService = routineService;
	}

	@GetMapping
	public List<RoutineResponse> findRoutines(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return routineService.findRoutines(userId)
			.stream()
			.map(RoutineResponse::from)
			.toList();
	}

	@PostMapping
	public ResponseEntity<RoutineResponse> createRoutine(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody RoutineCreateRequest request
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		RoutineSummary routine = routineService.createRoutine(
			userId,
			request.routineName(),
			request.routineMemo(),
			request.exercises()
		);

		return ResponseEntity
			.created(URI.create("/api/routines/" + routine.id()))
			.body(RoutineResponse.from(routine));
	}

	@GetMapping("/{routineId}")
	public RoutineDetailResponse findRoutineDetail(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer routineId
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return RoutineDetailResponse.from(routineService.findRoutineDetail(userId, routineId));
	}

	@PutMapping("/{routineId}")
	public RoutineResponse updateRoutine(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer routineId,
		@Valid @RequestBody RoutineCreateRequest request
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		RoutineSummary routine = routineService.updateRoutine(
			userId,
			routineId,
			request.routineName(),
			request.routineMemo(),
			request.exercises()
		);

		return RoutineResponse.from(routine);
	}

	@DeleteMapping("/{routineId}")
	public ResponseEntity<Void> deleteRoutine(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer routineId
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		routineService.deleteRoutine(userId, routineId);
		return ResponseEntity.noContent().build();
	}
}
