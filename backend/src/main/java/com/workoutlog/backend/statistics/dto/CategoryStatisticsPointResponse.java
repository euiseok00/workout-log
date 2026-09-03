package com.workoutlog.backend.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CategoryStatisticsPointResponse(
	LocalDate date,
	BigDecimal volume
) {
}
