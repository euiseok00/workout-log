package com.workoutlog.backend.exercise.service;

import java.util.List;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseOperationException;
import com.workoutlog.backend.exercise.ExerciseType;
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
	public List<Exercise> findActiveExercises(ExerciseCategory category) {
		return exerciseRepository.findActive(category);
	}

	@Transactional
	public Exercise createCustomExercise(String name, ExerciseCategory category) {
		return exerciseRepository.saveCustom(name, category);
	}

	@Transactional
	public Exercise updateCustomExercise(Integer exerciseId, String name, ExerciseCategory category) {
		Exercise exercise = getExercise(exerciseId);
		validateCustomExercise(exercise);
		validateActiveExercise(exercise);

		return exerciseRepository.update(exerciseId, name, category);
	}

	@Transactional
	public void deactivateCustomExercise(Integer exerciseId) {
		Exercise exercise = getExercise(exerciseId);
		validateCustomExercise(exercise);

		if (exercise.active()) {
			exerciseRepository.deactivate(exerciseId);
		}
	}

	private Exercise getExercise(Integer exerciseId) {
		return exerciseRepository.findById(exerciseId)
			.orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
	}

	private void validateCustomExercise(Exercise exercise) {
		if (exercise.type() != ExerciseType.CUSTOM) {
			throw new ExerciseOperationException("Only custom exercises can be changed.");
		}
	}

	private void validateActiveExercise(Exercise exercise) {
		if (!exercise.active()) {
			throw new ExerciseOperationException("Inactive exercises cannot be changed.");
		}
	}
}
