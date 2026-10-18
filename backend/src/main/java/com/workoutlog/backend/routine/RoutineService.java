package com.workoutlog.backend.routine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
import com.workoutlog.backend.workout.WorkoutSession;
import com.workoutlog.backend.workout.WorkoutSessionNotFoundException;
import com.workoutlog.backend.workout.WorkoutSessionRepository;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;

@Service
public class RoutineService {

	private final RoutineRepository routineRepository;
	private final ExerciseRepository exerciseRepository;
	private final WorkoutSessionRepository workoutSessionRepository;

	public RoutineService(RoutineRepository routineRepository, ExerciseRepository exerciseRepository,
			WorkoutSessionRepository workoutSessionRepository) {
		this.routineRepository = routineRepository;
		this.exerciseRepository = exerciseRepository;
		this.workoutSessionRepository = workoutSessionRepository;
	}

	@Transactional
	public RoutineResponse createFromSession(Integer sessionId, String name, Integer userId) {
		WorkoutSession session = workoutSessionRepository.findByIdAndUserId(sessionId, userId)
				.orElseThrow(WorkoutSessionNotFoundException::new);
		if (session.getRoutineId() != null) {
			throw new RequestValidationException("sessionId", "자유 운동 기록만 루틴으로 저장할 수 있습니다.");
		}
		Routine routine = new Routine(userId, name);
		for (ExerciseInSession exercise : session.getExercises()) {
			List<ExerciseSet> sets = new ArrayList<>(exercise.getSets());
			sets.sort(java.util.Comparator.comparing(ExerciseSet::getSetNumber));
			ExerciseInRoutine configured = new ExerciseInRoutine(routine, exercise.getExercise(),
					exercise.getExerciseOrder(), sets.size(), sets.isEmpty() ? null : sets.get(0).getReps());
			for (ExerciseSet set : sets) {
				configured.addSet(new RoutineSet(configured, set.getSetNumber(), set.getWeight(), set.getReps()));
			}
			routine.addExercise(configured);
		}
		return RoutineResponse.from(routineRepository.save(routine));
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

	@Transactional
	public void deleteRoutine(Integer routineId, Integer userId) {
		routineRepository.delete(findOwnedRoutine(routineId, userId));
	}

	private Routine findOwnedRoutine(Integer routineId, Integer userId) {
		return routineRepository.findByIdAndUserId(routineId, userId).orElseThrow(RoutineNotFoundException::new);
	}

	private Map<Integer, Exercise> validateAndLoadExercises(List<ExerciseRequest> requests, Integer userId) {
		HashSet<Integer> exerciseIds = new HashSet<>();
		HashSet<Integer> exerciseOrders = new HashSet<>();
		for (ExerciseRequest request : requests) {
			exerciseIds.add(request.exerciseId());
			if (!exerciseOrders.add(request.exerciseOrder())) {
				throw new RequestValidationException("exercises", "운동 순서는 중복될 수 없습니다.");
			}
			if (request.sets() != null) {
				HashSet<Integer> setNumbers = new HashSet<>();
				for (RoutineRequest.SetRequest set : request.sets()) {
					if (!setNumbers.add(set.setNumber())) {
						throw new RequestValidationException("sets", "세트 번호는 중복될 수 없습니다.");
					}
				}
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
		requests.forEach(request -> {
			ExerciseInRoutine configured = new ExerciseInRoutine(routine,
					exercises.get(request.exerciseId()), request.exerciseOrder(), request.targetSets(), request.targetReps());
			if (request.sets() != null) {
				request.sets().forEach(set -> configured.addSet(new RoutineSet(configured,
						set.setNumber(), set.weight(), set.reps())));
			}
			routine.addExercise(configured);
		});
	}
}
