package com.workoutlog.backend.exercise;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.exercise.dto.ExerciseRequest;
import com.workoutlog.backend.exercise.dto.ExerciseResponse;

@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;

	public ExerciseService(ExerciseRepository exerciseRepository) {
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional(readOnly = true)
	public List<ExerciseResponse> getExercises(Integer userId) {
		return exerciseRepository.findByUserIdIsNullOrUserIdOrderById(userId).stream()
				.map(ExerciseResponse::from)
				.toList();
	}

	@Transactional
	public ExerciseResponse createExercise(ExerciseRequest request, Integer userId) {
		Exercise exercise = exerciseRepository.save(new Exercise(request.name(), request.bodyPart(), userId));
		return ExerciseResponse.from(exercise);
	}
}
