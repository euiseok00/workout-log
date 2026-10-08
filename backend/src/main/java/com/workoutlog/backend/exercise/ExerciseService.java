package com.workoutlog.backend.exercise;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workoutlog.backend.exercise.dto.ExerciseRequest;
import com.workoutlog.backend.exercise.dto.ExerciseRecordResponse;
import com.workoutlog.backend.exercise.dto.ExerciseResponse;
import com.workoutlog.backend.workout.WorkoutSessionRepository;

@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;
	private final WorkoutSessionRepository workoutSessionRepository;

	public ExerciseService(ExerciseRepository exerciseRepository, WorkoutSessionRepository workoutSessionRepository) {
		this.exerciseRepository = exerciseRepository;
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
		Exercise exercise = exerciseRepository.findById(exerciseId)
				.filter(value -> value.getUserId() == null || value.getUserId().equals(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
}
