package com.workoutlog.backend.routine;

import java.util.List;

import com.workoutlog.backend.exercise.ExerciseCategory;

public record RoutineExerciseDetail(
	Integer exerciseId,
	String exerciseName,
	ExerciseCategory exerciseCategory,
	Integer exerciseOrder,
	String memo,
	List<RoutineSetDetail> sets
) {
}
