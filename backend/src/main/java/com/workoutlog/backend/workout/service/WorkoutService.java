package com.workoutlog.backend.workout.service;

import java.time.LocalDate;
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
import com.workoutlog.backend.routine.RoutineExerciseDetail;
import com.workoutlog.backend.routine.RoutineNotFoundException;
import com.workoutlog.backend.routine.RoutineSetDetail;
import com.workoutlog.backend.routine.repository.RoutineRepository;
import com.workoutlog.backend.workout.WorkoutExerciseResponse;
import com.workoutlog.backend.workout.WorkoutNotFoundException;
import com.workoutlog.backend.workout.WorkoutOperationException;
import com.workoutlog.backend.workout.WorkoutResponse;
import com.workoutlog.backend.workout.WorkoutSetResponse;
import com.workoutlog.backend.workout.WorkoutSetType;
import com.workoutlog.backend.workout.WorkoutSummaryResponse;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest.WorkoutExerciseRequest;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest.WorkoutSetRequest;
import com.workoutlog.backend.workout.repository.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutService {
	private final WorkoutRepository workoutRepository;
	private final ExerciseRepository exerciseRepository;
	private final RoutineRepository routineRepository;

	public WorkoutService(
		WorkoutRepository workoutRepository,
		ExerciseRepository exerciseRepository,
		RoutineRepository routineRepository
	) {
		this.workoutRepository = workoutRepository;
		this.exerciseRepository = exerciseRepository;
		this.routineRepository = routineRepository;
	}

	@Transactional
	public WorkoutResponse createWorkout(
		UUID userId,
		LocalDate workoutDate,
		String memo,
		List<WorkoutExerciseRequest> workoutExercises
	) {
		Integer workoutOrder = workoutRepository.findNextWorkoutOrder(userId, workoutDate);
		List<ExerciseToSave> exercises = validateWorkoutExercises(userId, workoutExercises);
		Integer workoutId = workoutRepository.saveWorkout(userId, workoutDate, workoutOrder, memo);

		for (ExerciseToSave exercise : exercises) {
			Integer workoutExerciseId = workoutRepository.saveWorkoutExercise(
				workoutId,
				exercise.exercise().id(),
				exercise.exercise().name(),
				exercise.exerciseOrder(),
				exercise.memo(),
				exercise.completed()
			);

			for (WorkoutSetRequest set : exercise.sets()) {
				workoutRepository.saveWorkoutSet(
					workoutExerciseId,
					set.setOrder(),
					set.weight(),
					set.reps(),
					set.rpe(),
					set.setType(),
					Boolean.TRUE.equals(set.completed())
				);
			}
		}

		return new WorkoutResponse(
			workoutId,
			workoutDate,
			workoutOrder,
			memo,
			exercises.stream()
				.sorted(Comparator.comparing(ExerciseToSave::exerciseOrder))
				.map(this::toWorkoutExerciseResponse)
				.toList()
		);
	}

	@Transactional
	public WorkoutResponse createWorkoutFromRoutine(
		UUID userId,
		Integer routineId,
		LocalDate workoutDate,
		String memo
	) {
		Integer workoutOrder = workoutRepository.findNextWorkoutOrder(userId, workoutDate);
		RoutineDetail routine = routineRepository.findDetailById(userId, routineId)
			.orElseThrow(() -> new RoutineNotFoundException(routineId));
		validateRoutineExercises(userId, routine.exercises());
		Integer workoutId = workoutRepository.saveWorkout(userId, workoutDate, workoutOrder, memo);
		List<WorkoutExerciseResponse> exerciseResponses = new ArrayList<>();

		for (RoutineExerciseDetail routineExercise : routine.exercises()) {
			Integer workoutExerciseId = workoutRepository.saveWorkoutExercise(
				workoutId,
				routineExercise.exerciseId(),
				routineExercise.exerciseName(),
				routineExercise.exerciseOrder(),
				routineExercise.memo(),
				false
			);

			List<WorkoutSetResponse> setResponses = new ArrayList<>();
			for (RoutineSetDetail set : routineExercise.sets()) {
				WorkoutSetType setType = WorkoutSetType.valueOf(set.setType().name());
				workoutRepository.saveWorkoutSet(
					workoutExerciseId,
					set.setOrder(),
					set.weight(),
					set.reps(),
					null,
					setType,
					false
				);
				setResponses.add(new WorkoutSetResponse(
					set.setOrder(),
					set.weight(),
					set.reps(),
					null,
					setType,
					false
				));
			}

			exerciseResponses.add(new WorkoutExerciseResponse(
				routineExercise.exerciseId(),
				routineExercise.exerciseName(),
				routineExercise.exerciseCategory(),
				routineExercise.exerciseOrder(),
				routineExercise.memo(),
				false,
				setResponses
			));
		}

		return new WorkoutResponse(
			workoutId,
			workoutDate,
			workoutOrder,
			memo,
			exerciseResponses
		);
	}

	@Transactional(readOnly = true)
	public WorkoutResponse findWorkout(UUID userId, Integer workoutId) {
		return workoutRepository.findById(userId, workoutId)
			.orElseThrow(() -> new WorkoutNotFoundException(workoutId));
	}

	@Transactional(readOnly = true)
	public List<LocalDate> findWorkoutDates(UUID userId, Integer year, Integer month) {
		LocalDate startDate = LocalDate.of(year, month, 1);
		LocalDate nextMonthStartDate = startDate.plusMonths(1);

		return workoutRepository.findWorkoutDates(userId, startDate, nextMonthStartDate);
	}

	@Transactional(readOnly = true)
	public List<WorkoutSummaryResponse> findWorkoutSummariesByDate(UUID userId, LocalDate date) {
		return workoutRepository.findSummariesByDate(userId, date);
	}

	@Transactional
	public void deleteWorkout(UUID userId, Integer workoutId) {
		int deletedCount = workoutRepository.deleteById(userId, workoutId);
		if (deletedCount == 0) {
			throw new WorkoutNotFoundException(workoutId);
		}
	}

	private List<ExerciseToSave> validateWorkoutExercises(
		UUID userId,
		List<WorkoutExerciseRequest> workoutExercises
	) {
		Set<Integer> exerciseOrders = new HashSet<>();
		List<ExerciseToSave> exercises = new ArrayList<>();

		for (WorkoutExerciseRequest workoutExercise : workoutExercises) {
			if (!exerciseOrders.add(workoutExercise.exerciseOrder())) {
				throw new WorkoutOperationException("Exercise order cannot be duplicated in a workout.");
			}

			validateSetOrders(workoutExercise.sets());

			Exercise exercise = exerciseRepository.findAvailableById(userId, workoutExercise.exerciseId())
				.orElseThrow(() -> new ExerciseNotFoundException(workoutExercise.exerciseId()));

			if (!exercise.active()) {
				throw new WorkoutOperationException("Inactive exercises cannot be added to workouts.");
			}

			exercises.add(new ExerciseToSave(
				exercise,
				workoutExercise.exerciseOrder(),
				workoutExercise.memo(),
				Boolean.TRUE.equals(workoutExercise.completed()),
				workoutExercise.sets()
			));
		}

		return exercises;
	}

	private void validateRoutineExercises(UUID userId, List<RoutineExerciseDetail> routineExercises) {
		for (RoutineExerciseDetail routineExercise : routineExercises) {
			Exercise exercise = exerciseRepository.findAvailableById(userId, routineExercise.exerciseId())
				.orElseThrow(() -> new ExerciseNotFoundException(routineExercise.exerciseId()));

			if (!exercise.active()) {
				throw new WorkoutOperationException("Inactive exercises cannot be added to workouts.");
			}
		}
	}

	private void validateSetOrders(List<WorkoutSetRequest> sets) {
		Set<Integer> setOrders = new HashSet<>();

		for (WorkoutSetRequest set : sets) {
			if (!setOrders.add(set.setOrder())) {
				throw new WorkoutOperationException("Set order cannot be duplicated in an exercise.");
			}
		}
	}

	private WorkoutExerciseResponse toWorkoutExerciseResponse(ExerciseToSave exercise) {
		return new WorkoutExerciseResponse(
			exercise.exercise().id(),
			exercise.exercise().name(),
			exercise.exercise().category(),
			exercise.exerciseOrder(),
			exercise.memo(),
			exercise.completed(),
			exercise.sets()
				.stream()
				.sorted(Comparator.comparing(WorkoutSetRequest::setOrder))
				.map(set -> new WorkoutSetResponse(
					set.setOrder(),
					set.weight(),
					set.reps(),
					set.rpe(),
					set.setType(),
					Boolean.TRUE.equals(set.completed())
				))
				.toList()
		);
	}

	private record ExerciseToSave(
		Exercise exercise,
		Integer exerciseOrder,
		String memo,
		boolean completed,
		List<WorkoutSetRequest> sets
	) {
	}
}
