package com.workoutlog.backend.routine.controller;

import java.net.URI;
import java.util.List;

import com.workoutlog.backend.routine.RoutineSummary;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest;
import com.workoutlog.backend.routine.dto.RoutineResponse;
import com.workoutlog.backend.routine.service.RoutineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
	public List<RoutineResponse> findRoutines() {
		return routineService.findRoutines()
			.stream()
			.map(RoutineResponse::from)
			.toList();
	}

	@PostMapping
	public ResponseEntity<RoutineResponse> createRoutine(
		@Valid @RequestBody RoutineCreateRequest request
	) {
		RoutineSummary routine = routineService.createRoutine(
			request.routineName(),
			request.routineMemo(),
			request.exercises()
		);

		return ResponseEntity
			.created(URI.create("/api/routines/" + routine.id()))
			.body(RoutineResponse.from(routine));
	}
}
