package com.workoutlog.backend.routine;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseLookup;
import com.workoutlog.backend.routine.dto.RoutineRequest;
import com.workoutlog.backend.routine.dto.RoutineRequest.RoutineExerciseRequest;
import com.workoutlog.backend.routine.dto.RoutineRequest.RoutineSetRequest;
import com.workoutlog.backend.routine.dto.RoutineResponse;
import com.workoutlog.backend.workout.WorkoutSession;
import com.workoutlog.backend.workout.WorkoutSessionNotFoundException;
import com.workoutlog.backend.workout.WorkoutSessionRepository;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;

@Service
public class RoutineService {

	private final RoutineRepository routineRepository;
	private final ExerciseLookup exerciseLookup;
	private final WorkoutSessionRepository workoutSessionRepository;

	public RoutineService(RoutineRepository routineRepository, ExerciseLookup exerciseLookup,
			WorkoutSessionRepository workoutSessionRepository) {
		this.routineRepository = routineRepository;
		this.exerciseLookup = exerciseLookup;
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
			sets.sort(Comparator.comparing(ExerciseSet::getSetNumber));
			ExerciseInRoutine configured = new ExerciseInRoutine(routine, exercise.getExercise(),
					exercise.getExerciseOrder(), sets.isEmpty() ? null : sets.size(), sets.isEmpty() ? null : sets.get(0).getReps());
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

	private Map<Integer, Exercise> validateAndLoadExercises(List<RoutineExerciseRequest> requests, Integer userId) {
		RequestValidationException.requireDistinct(
				requests.stream().map(RoutineExerciseRequest::exerciseOrder).toList(), "exercises",
				"운동 순서는 중복될 수 없습니다.");
		for (RoutineExerciseRequest request : requests) {
			if (request.sets() != null) {
				RequestValidationException.requireDistinct(
						request.sets().stream().map(RoutineSetRequest::setNumber).toList(), "sets",
						"세트 번호는 중복될 수 없습니다.");
			}
		}
		return exerciseLookup.loadUsable(
				requests.stream().map(RoutineExerciseRequest::exerciseId).collect(Collectors.toSet()), userId);
	}

	// 세트가 있으면 목표 세트·반복 수는 세트에서 파생한다(두 정보가 어긋나지 않게).
	private void addExercises(Routine routine, List<RoutineExerciseRequest> requests,
			Map<Integer, Exercise> exercises) {
		requests.forEach(request -> {
			List<RoutineSetRequest> sets = request.sets() == null ? List.of() : request.sets();
			Integer targetSets = request.targetSets();
			Integer targetReps = request.targetReps();
			if (!sets.isEmpty()) {
				targetSets = sets.size();
				int firstReps = sets.stream().min(Comparator.comparing(RoutineSetRequest::setNumber)).get().reps();
				targetReps = firstReps > 0 ? firstReps : null;
			}
			ExerciseInRoutine configured = new ExerciseInRoutine(routine, exercises.get(request.exerciseId()),
					request.exerciseOrder(), targetSets, targetReps);
			sets.forEach(set -> configured.addSet(new RoutineSet(configured, set.setNumber(), set.weight(), set.reps())));
			routine.addExercise(configured);
		});
	}
}
