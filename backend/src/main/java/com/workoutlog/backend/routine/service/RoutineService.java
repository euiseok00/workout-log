package com.workoutlog.backend.routine.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.repository.ExerciseRepository;
import com.workoutlog.backend.routine.RoutineDetail;
import com.workoutlog.backend.routine.RoutineNotFoundException;
import com.workoutlog.backend.routine.RoutineOperationException;
import com.workoutlog.backend.routine.RoutineSummary;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest.RoutineExerciseRequest;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest.RoutineSetRequest;
import com.workoutlog.backend.routine.repository.RoutineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {
	private final RoutineRepository routineRepository;
	private final ExerciseRepository exerciseRepository;

	public RoutineService(RoutineRepository routineRepository, ExerciseRepository exerciseRepository) {
		this.routineRepository = routineRepository;
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional(readOnly = true)
	public List<RoutineSummary> findRoutines(UUID userId) {
		return routineRepository.findAll(userId);
	}

	@Transactional(readOnly = true)
	public RoutineDetail findRoutineDetail(UUID userId, Integer routineId) {
		return routineRepository.findDetailById(userId, routineId)
			.orElseThrow(() -> new RoutineNotFoundException(routineId));
	}

	@Transactional
	public RoutineSummary createRoutine(
		UUID userId,
		String routineName,
		String routineMemo,
		List<RoutineExerciseRequest> routineExercises
	) {
		List<ExerciseWithOrder> exercises = validateRoutineExercises(userId, routineExercises);
		Integer routineId = routineRepository.saveRoutine(userId, routineName, routineMemo);
		saveRoutineDetails(routineId, routineExercises);

		return toRoutineSummary(routineId, routineName, routineMemo, exercises);
	}

	@Transactional
	public RoutineSummary updateRoutine(
		UUID userId,
		Integer routineId,
		String routineName,
		String routineMemo,
		List<RoutineExerciseRequest> routineExercises
	) {
		validateRoutineExists(userId, routineId);
		List<ExerciseWithOrder> exercises = validateRoutineExercises(userId, routineExercises);
		routineRepository.updateRoutine(userId, routineId, routineName, routineMemo);
		routineRepository.deleteRoutineExercisesByRoutineId(routineId);
		saveRoutineDetails(routineId, routineExercises);

		return toRoutineSummary(routineId, routineName, routineMemo, exercises);
	}

	@Transactional
	public void deleteRoutine(UUID userId, Integer routineId) {
		int deletedCount = routineRepository.deleteById(userId, routineId);
		if (deletedCount == 0) {
			throw new RoutineNotFoundException(routineId);
		}
	}

	private void saveRoutineDetails(
		Integer routineId,
		List<RoutineExerciseRequest> routineExercises
	) {
		for (RoutineExerciseRequest routineExercise : routineExercises) {
			Integer routineExerciseId = routineRepository.saveRoutineExercise(
				routineId,
				routineExercise.exerciseId(),
				routineExercise.exerciseOrder(),
				routineExercise.memo()
			);

			for (RoutineSetRequest set : routineExercise.sets()) {
				routineRepository.saveRoutineSet(
					routineExerciseId,
					set.setOrder(),
					set.weight(),
					set.reps(),
					set.setType()
				);
			}
		}
	}

	private RoutineSummary toRoutineSummary(
		Integer routineId,
		String routineName,
		String routineMemo,
		List<ExerciseWithOrder> exercises
	) {
		return new RoutineSummary(
			routineId,
			routineName,
			routineMemo,
			exercises.stream()
				.sorted(Comparator.comparing(ExerciseWithOrder::exerciseOrder))
				.map(ExerciseWithOrder::exercise)
				.map(Exercise::name)
				.toList()
		);
	}

	private void validateRoutineExists(UUID userId, Integer routineId) {
		if (!routineRepository.existsById(userId, routineId)) {
			throw new RoutineNotFoundException(routineId);
		}
	}

	private List<ExerciseWithOrder> validateRoutineExercises(
		UUID userId,
		List<RoutineExerciseRequest> routineExercises
	) {
		Set<Integer> exerciseOrders = new HashSet<>();
		List<ExerciseWithOrder> exercises = new ArrayList<>();

		for (RoutineExerciseRequest routineExercise : routineExercises) {
			if (!exerciseOrders.add(routineExercise.exerciseOrder())) {
				throw new RoutineOperationException("Exercise order cannot be duplicated in a routine.");
			}

			validateSetOrders(routineExercise.sets());

			Exercise exercise = exerciseRepository.findAvailableById(userId, routineExercise.exerciseId())
				.orElseThrow(() -> new ExerciseNotFoundException(routineExercise.exerciseId()));

			if (!exercise.active()) {
				throw new RoutineOperationException("Inactive exercises cannot be added to routines.");
			}

			exercises.add(new ExerciseWithOrder(exercise, routineExercise.exerciseOrder()));
		}

		return exercises;
	}

	private void validateSetOrders(List<RoutineSetRequest> sets) {
		Set<Integer> setOrders = new HashSet<>();

		for (RoutineSetRequest set : sets) {
			if (!setOrders.add(set.setOrder())) {
				throw new RoutineOperationException("Set order cannot be duplicated in an exercise.");
			}
		}
	}

	private record ExerciseWithOrder(
		Exercise exercise,
		Integer exerciseOrder
	) {
	}
}
