package com.workoutlog.backend.workout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseType;
import com.workoutlog.backend.exercise.repository.ExerciseRepository;
import com.workoutlog.backend.routine.RoutineDetail;
import com.workoutlog.backend.routine.RoutineExerciseDetail;
import com.workoutlog.backend.routine.RoutineNotFoundException;
import com.workoutlog.backend.routine.RoutineSetDetail;
import com.workoutlog.backend.routine.RoutineSetType;
import com.workoutlog.backend.routine.repository.RoutineRepository;
import com.workoutlog.backend.workout.WorkoutOperationException;
import com.workoutlog.backend.workout.WorkoutResponse;
import com.workoutlog.backend.workout.WorkoutSetType;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest.WorkoutExerciseRequest;
import com.workoutlog.backend.workout.dto.WorkoutCreateRequest.WorkoutSetRequest;
import com.workoutlog.backend.workout.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {
	private static final LocalDate WORKOUT_DATE = LocalDate.of(2026, 9, 1);

	@Mock
	private WorkoutRepository workoutRepository;

	@Mock
	private ExerciseRepository exerciseRepository;

	@Mock
	private RoutineRepository routineRepository;

	@InjectMocks
	private WorkoutService workoutService;

	@Test
	void createWorkoutSavesExerciseNameSnapshotAndSets() {
		WorkoutExerciseRequest exerciseRequest = new WorkoutExerciseRequest(
			1,
			1,
			"벤치 메모",
			true,
			List.of(set(1, 8, true), set(2, null, false))
		);
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(2);
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.SYSTEM,
				ExerciseCategory.CHEST,
				true
			)));
		when(workoutRepository.saveWorkout(WORKOUT_DATE, 2, "오늘 기록"))
			.thenReturn(10);
		when(workoutRepository.saveWorkoutExercise(10, 1, "벤치프레스", 1, "벤치 메모", true))
			.thenReturn(20);

		WorkoutResponse workout = workoutService.createWorkout(
			WORKOUT_DATE,
			"오늘 기록",
			List.of(exerciseRequest)
		);

		assertEquals(10, workout.workoutId());
		assertEquals(2, workout.workoutOrder());
		assertEquals("벤치프레스", workout.exercises().getFirst().exerciseName());
		verify(workoutRepository).saveWorkoutSet(20, 1, BigDecimal.valueOf(80), 10, 8, WorkoutSetType.WORKING, true);
		verify(workoutRepository).saveWorkoutSet(20, 2, BigDecimal.valueOf(80), 10, null, WorkoutSetType.WORKING, false);
	}

	@Test
	void createWorkoutRejectsDuplicateExerciseOrder() {
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.SYSTEM,
				ExerciseCategory.CHEST,
				true
			)));

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(
				WORKOUT_DATE,
				null,
				List.of(exercise(1, 1), exercise(2, 1))
			)
		);

		verify(workoutRepository, never()).saveWorkout(WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutRejectsUnknownExercise() {
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findById(99))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> workoutService.createWorkout(WORKOUT_DATE, null, List.of(exercise(99, 1)))
		);

		verify(workoutRepository, never()).saveWorkout(WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutRejectsInactiveExercise() {
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(WORKOUT_DATE, null, List.of(exercise(1, 1)))
		);

		verify(workoutRepository, never()).saveWorkout(WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutRejectsDuplicateSetOrder() {
		WorkoutExerciseRequest exerciseRequest = new WorkoutExerciseRequest(
			1,
			1,
			null,
			false,
			List.of(set(1, null, false), set(1, null, false))
		);
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(1);

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(WORKOUT_DATE, null, List.of(exerciseRequest))
		);

		verify(workoutRepository, never()).saveWorkout(WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutFromRoutineCopiesRoutineDetailsWithWorkoutDefaults() {
		RoutineDetail routine = new RoutineDetail(
			5,
			"Push A",
			"가슴 중심",
			List.of(new RoutineExerciseDetail(
				1,
				"벤치프레스",
				ExerciseCategory.CHEST,
				1,
				"벤치 메모",
				List.of(new RoutineSetDetail(
					1,
					BigDecimal.valueOf(80),
					8,
					RoutineSetType.WORKING
				))
			))
		);
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(3);
		when(routineRepository.findDetailById(5))
			.thenReturn(Optional.of(routine));
		when(workoutRepository.saveWorkout(WORKOUT_DATE, 3, "오늘 기록"))
			.thenReturn(10);
		when(workoutRepository.saveWorkoutExercise(10, 1, "벤치프레스", 1, "벤치 메모", false))
			.thenReturn(20);

		WorkoutResponse workout = workoutService.createWorkoutFromRoutine(5, WORKOUT_DATE, "오늘 기록");

		assertEquals(10, workout.workoutId());
		assertEquals(3, workout.workoutOrder());
		assertEquals(false, workout.exercises().getFirst().completed());
		assertEquals(null, workout.exercises().getFirst().sets().getFirst().rpe());
		verify(workoutRepository).saveWorkoutSet(20, 1, BigDecimal.valueOf(80), 8, null, WorkoutSetType.WORKING, false);
	}

	@Test
	void createWorkoutFromRoutineRejectsUnknownRoutine() {
		when(workoutRepository.findNextWorkoutOrder(WORKOUT_DATE))
			.thenReturn(1);
		when(routineRepository.findDetailById(5))
			.thenReturn(Optional.empty());

		assertThrows(
			RoutineNotFoundException.class,
			() -> workoutService.createWorkoutFromRoutine(5, WORKOUT_DATE, null)
		);

		verify(workoutRepository, never()).saveWorkout(WORKOUT_DATE, 1, null);
	}

	private WorkoutExerciseRequest exercise(Integer exerciseId, Integer exerciseOrder) {
		return new WorkoutExerciseRequest(
			exerciseId,
			exerciseOrder,
			null,
			false,
			List.of(set(1, null, false))
		);
	}

	private WorkoutSetRequest set(Integer setOrder, Integer rpe, boolean completed) {
		return new WorkoutSetRequest(
			setOrder,
			BigDecimal.valueOf(80),
			10,
			rpe,
			WorkoutSetType.WORKING,
			completed
		);
	}
}
