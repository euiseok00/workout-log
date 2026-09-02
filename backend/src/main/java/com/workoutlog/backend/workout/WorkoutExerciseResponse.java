package com.workoutlog.backend.workout;

import java.util.List;

import com.workoutlog.backend.exercise.ExerciseCategory;

public record WorkoutExerciseResponse(
	Integer exerciseId,
	String exerciseName,
	ExerciseCategory exerciseCategory,
	Integer exerciseOrder,
	String memo,
	boolean completed,
	List<WorkoutSetResponse> sets
) {
}
