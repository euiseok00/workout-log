package com.workoutlog.backend.routine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.routine.dto.RoutineRequest;
import com.workoutlog.backend.routine.dto.RoutineRequest.ExerciseRequest;
import com.workoutlog.backend.routine.dto.RoutineResponse;

@Service
public class RoutineService {

	private final RoutineRepository routineRepository;
	private final ExerciseRepository exerciseRepository;

	public RoutineService(RoutineRepository routineRepository, ExerciseRepository exerciseRepository) {
		this.routineRepository = routineRepository;
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional
	public RoutineResponse createRoutine(RoutineRequest request, Integer userId) {
		Map<Integer, Exercise> exercises = validateAndLoadExercises(request.exercises(), userId);
		Routine routine = new Routine(userId, request.name());
		addExercises(routine, request.exercises(), exercises);
		return RoutineResponse.from(routineRepository.save(routine));
	}

	@Transactional(readOnly = true)
	public List<RoutineResponse> getRoutines(Integer userId) {
		return routineRepository.findAllByUserIdOrderById(userId).stream()
				.map(RoutineResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public RoutineResponse getRoutine(Integer routineId, Integer userId) {
		return RoutineResponse.from(findOwnedRoutine(routineId, userId));
	}

	@Transactional
	public RoutineResponse updateRoutine(Integer routineId, RoutineRequest request, Integer userId) {
		Routine routine = findOwnedRoutine(routineId, userId);
		Map<Integer, Exercise> exercises = validateAndLoadExercises(request.exercises(), userId);
		routine.update(request.name());
		routine.clearExercises();
		routineRepository.flush();
		addExercises(routine, request.exercises(), exercises);
		return RoutineResponse.from(routine);
	}

	private Routine findOwnedRoutine(Integer routineId, Integer userId) {
		return routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
	}

	private Map<Integer, Exercise> validateAndLoadExercises(List<ExerciseRequest> requests, Integer userId) {
		HashSet<Integer> exerciseIds = new HashSet<>();
		HashSet<Integer> exerciseOrders = new HashSet<>();
		for (ExerciseRequest request : requests) {
			if (!exerciseIds.add(request.exerciseId())) {
				throw new RequestValidationException("exercises", "같은 운동을 중복해서 추가할 수 없습니다.");
			}
			if (!exerciseOrders.add(request.exerciseOrder())) {
				throw new RequestValidationException("exercises", "운동 순서는 중복될 수 없습니다.");
			}
		}

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

	private void addExercises(Routine routine, List<ExerciseRequest> requests, Map<Integer, Exercise> exercises) {
		requests.forEach(request -> routine.addExercise(new ExerciseInRoutine(routine,
				exercises.get(request.exerciseId()), request.exerciseOrder(), request.targetSets(), request.targetReps())));
	}
}
