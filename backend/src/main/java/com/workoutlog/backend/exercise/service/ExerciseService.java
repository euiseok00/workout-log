package com.workoutlog.backend.exercise.service;

import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseOperationException;
import com.workoutlog.backend.exercise.repository.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {
	private final ExerciseRepository exerciseRepository;

	public ExerciseService(ExerciseRepository exerciseRepository) {
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional(readOnly = true)
	public List<Exercise> findExercises(UUID userId, ExerciseCategory category, Boolean active) {
		return exerciseRepository.findByFilters(userId, category, active);
	}

	@Transactional
	public Exercise createCustomExercise(UUID userId, String name, ExerciseCategory category) {
		return exerciseRepository.saveCustom(userId, name, category);
	}

	@Transactional
	public Exercise updateCustomExercise(UUID userId, Integer exerciseId, String name, ExerciseCategory category) {
		Exercise exercise = getCustomExercise(userId, exerciseId);
		validateActiveExercise(exercise);

		return exerciseRepository.update(userId, exerciseId, name, category);
	}

	@Transactional
	public void deactivateCustomExercise(UUID userId, Integer exerciseId) {
		Exercise exercise = getCustomExercise(userId, exerciseId);

		if (exercise.active()) {
			exerciseRepository.setActive(userId, exerciseId, false);
		}
	}

	@Transactional
	public void activateCustomExercise(UUID userId, Integer exerciseId) {
		Exercise exercise = getCustomExercise(userId, exerciseId);

		if (!exercise.active()) {
			exerciseRepository.setActive(userId, exerciseId, true);
		}
	}

	private Exercise getCustomExercise(UUID userId, Integer exerciseId) {
		return exerciseRepository.findCustomById(userId, exerciseId)
			.orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
	}

	private void validateActiveExercise(Exercise exercise) {
		if (!exercise.active()) {
			throw new ExerciseOperationException("Inactive exercises cannot be changed.");
		}
	}
}
