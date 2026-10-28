package com.workoutlog.backend.exercise;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.workoutlog.backend.common.error.RequestValidationException;

@Component
public class ExerciseLookup {

	private final ExerciseRepository exerciseRepository;

	public ExerciseLookup(ExerciseRepository exerciseRepository) {
		this.exerciseRepository = exerciseRepository;
	}

	public Exercise findVisible(Integer exerciseId, Integer userId) {
		return exerciseRepository.findById(exerciseId)
				.filter(value -> value.getUserId() == null || value.getUserId().equals(userId))
				.orElseThrow(ExerciseNotFoundException::new);
	}

	public Map<Integer, Exercise> loadUsable(Collection<Integer> exerciseIds, Integer userId) {
		Map<Integer, Exercise> exercises = exerciseRepository.findAllById(exerciseIds).stream()
				.collect(Collectors.toMap(Exercise::getId, Function.identity()));
		for (Integer exerciseId : exerciseIds) {
			Exercise exercise = exercises.get(exerciseId);
			if (exercise == null || exercise.getUserId() != null && !exercise.getUserId().equals(userId)) {
				throw new RequestValidationException("exercises", "사용할 수 없는 운동이 포함되어 있습니다.");
			}
		}
		return exercises;
	}
}
