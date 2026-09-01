package com.workoutlog.backend.routine;

import java.util.List;

public record RoutineSummary(
	Integer id,
	String name,
	String memo,
	List<String> exerciseNames
) {
	public int exerciseCount() {
		return exerciseNames.size();
	}
}
