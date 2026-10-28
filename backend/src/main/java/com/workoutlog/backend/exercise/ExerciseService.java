package com.workoutlog.backend.exercise;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.exercise.dto.ExerciseRequest;
import com.workoutlog.backend.exercise.dto.ExerciseRecordResponse;
import com.workoutlog.backend.exercise.dto.ExerciseResponse;
import com.workoutlog.backend.workout.WorkoutSessionRepository;

@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;
	private final ExerciseLookup exerciseLookup;
	private final WorkoutSessionRepository workoutSessionRepository;

	public ExerciseService(ExerciseRepository exerciseRepository, ExerciseLookup exerciseLookup,
			WorkoutSessionRepository workoutSessionRepository) {
		this.exerciseRepository = exerciseRepository;
		this.exerciseLookup = exerciseLookup;
		this.workoutSessionRepository = workoutSessionRepository;
	}

	@Transactional(readOnly = true)
	public List<ExerciseResponse> getExercises(Integer userId) {
		return exerciseRepository.findByUserIdIsNullOrUserIdOrderById(userId).stream()
				.map(ExerciseResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ExerciseRecordResponse> getExerciseRecords(Integer exerciseId, Integer userId) {
		Exercise exercise = exerciseLookup.findVisible(exerciseId, userId);
		return workoutSessionRepository.findAllInDateRange(userId, null, null).stream()
				.flatMap(session -> session.getExercises().stream()
						.filter(value -> value.getExercise().getId().equals(exerciseId))
						.map(value -> ExerciseRecordResponse.from(session, value, exercise)))
				.toList();
	}

	@Transactional
	public ExerciseResponse createExercise(ExerciseRequest request, Integer userId) {
		Exercise exercise = exerciseRepository.save(new Exercise(request.name(), request.bodyPart(), userId));
		return ExerciseResponse.from(exercise);
	}

	@Transactional
	public ExerciseResponse updateExercise(Integer exerciseId, ExerciseRequest request, Integer userId) {
		Exercise exercise = exerciseRepository.findById(exerciseId)
				.filter(value -> userId.equals(value.getUserId()))
				.orElseThrow(ExerciseNotFoundException::new);
		exercise.update(request.name(), request.bodyPart());
		return ExerciseResponse.from(exercise);
	}
}
