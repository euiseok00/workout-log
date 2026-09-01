package com.workoutlog.backend.routine;

import java.util.List;

public record RoutineDetail(
	Integer id,
	String name,
	String memo,
	List<RoutineExerciseDetail> exercises
) {
}
