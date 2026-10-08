package com.workoutlog.backend.statistics;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/summary")
	public StatisticsResponse.Summary getSummary(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@AuthenticationPrincipal Jwt jwt) {
		return statisticsService.getSummary(Integer.valueOf(jwt.getSubject()), from, to);
	}

	@GetMapping("/exercises/{exerciseId}")
	public StatisticsResponse.Exercise getExerciseStatistics(@PathVariable Integer exerciseId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@AuthenticationPrincipal Jwt jwt) {
		return statisticsService.getExerciseStatistics(exerciseId, Integer.valueOf(jwt.getSubject()), from, to);
	}
}
