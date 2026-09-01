package com.workoutlog.backend.routine;

import java.math.BigDecimal;

public record RoutineSetDetail(
	Integer setOrder,
	BigDecimal weight,
	Integer reps,
	RoutineSetType setType
) {
}
