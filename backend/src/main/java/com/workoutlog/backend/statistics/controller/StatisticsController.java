package com.workoutlog.backend.statistics.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.statistics.dto.CategoryStatisticsPointResponse;
import com.workoutlog.backend.statistics.dto.ExerciseStatisticsDateResponse;
import com.workoutlog.backend.statistics.service.StatisticsService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/exercises/{exerciseId}")
	public List<ExerciseStatisticsDateResponse> findExerciseStatistics(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable @Positive Integer exerciseId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return statisticsService.findExerciseStatistics(userId(jwt), exerciseId, from, to);
	}

	@GetMapping("/categories/{category}")
	public List<CategoryStatisticsPointResponse> findCategoryStatistics(
		@AuthenticationPrincipal Jwt jwt,
		@PathVariable ExerciseCategory category,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return statisticsService.findCategoryStatistics(userId(jwt), category, from, to);
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
