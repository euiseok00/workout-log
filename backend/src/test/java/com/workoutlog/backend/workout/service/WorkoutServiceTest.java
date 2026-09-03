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
import java.util.UUID;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {
	private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
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
	void createWorkoutSavesExerciseSnapshotAndSets() {
		WorkoutExerciseRequest exerciseRequest = new WorkoutExerciseRequest(
			1,
			1,
			"벤치 메모",
			true,
			List.of(set(1, 8, true), set(2, null, false))
		);
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(2);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(workoutRepository.saveWorkout(USER_A, WORKOUT_DATE, 2, "오늘 기록"))
			.thenReturn(10);
		when(workoutRepository.saveWorkoutExercise(10, 1, "벤치프레스", ExerciseCategory.CHEST, 1, "벤치 메모", true))
			.thenReturn(20);

		WorkoutResponse workout = workoutService.createWorkout(
			USER_A,
			WORKOUT_DATE,
			"오늘 기록",
			List.of(exerciseRequest)
		);

		assertEquals(10, workout.workoutId());
		assertEquals(2, workout.workoutOrder());
		assertEquals("벤치프레스", workout.exercises().getFirst().exerciseName());
		assertEquals(ExerciseCategory.CHEST, workout.exercises().getFirst().exerciseCategory());
		verify(workoutRepository).saveWorkout(USER_A, WORKOUT_DATE, 2, "오늘 기록");
		verify(workoutRepository).saveWorkoutSet(20, 1, BigDecimal.valueOf(80), 10, 8, WorkoutSetType.WORKING, true);
		verify(workoutRepository).saveWorkoutSet(20, 2, BigDecimal.valueOf(80), 10, null, WorkoutSetType.WORKING, false);
	}

	@Test
	void createWorkoutAllowsSystemExercise() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(workoutRepository.saveWorkout(USER_A, WORKOUT_DATE, 1, null))
			.thenReturn(10);

		WorkoutResponse workout = workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(1, 1)));

		assertEquals("벤치프레스", workout.exercises().getFirst().exerciseName());
	}

	@Test
	void createWorkoutAllowsOwnedCustomExercise() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 2))
			.thenReturn(Optional.of(new Exercise(
				2,
				"My Row",
				ExerciseType.CUSTOM,
				ExerciseCategory.BACK,
				true
			)));
		when(workoutRepository.saveWorkout(USER_A, WORKOUT_DATE, 1, null))
			.thenReturn(10);

		WorkoutResponse workout = workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(2, 1)));

		assertEquals("My Row", workout.exercises().getFirst().exerciseName());
	}

	@Test
	void createWorkoutRejectsOtherUserCustomExercise() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(2, 1)))
		);

		verify(workoutRepository, never()).saveWorkout(USER_A, WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutUsesUserSpecificWorkoutOrder() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(workoutRepository.saveWorkout(USER_A, WORKOUT_DATE, 1, null))
			.thenReturn(10);

		WorkoutResponse workout = workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(1, 1)));

		assertEquals(1, workout.workoutOrder());
		verify(workoutRepository).findNextWorkoutOrder(USER_A, WORKOUT_DATE);
		verify(workoutRepository, never()).findNextWorkoutOrder(USER_B, WORKOUT_DATE);
	}

	@Test
	void createWorkoutRejectsDuplicateExerciseOrder() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(
				USER_A,
				WORKOUT_DATE,
				null,
				List.of(exercise(1, 1), exercise(2, 1))
			)
		);

		verify(workoutRepository, never()).saveWorkout(USER_A, WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutRejectsUnknownExercise() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 99))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(99, 1)))
		);

		verify(workoutRepository, never()).saveWorkout(USER_A, WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutRejectsInactiveExercise() {
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exercise(1, 1)))
		);

		verify(workoutRepository, never()).saveWorkout(USER_A, WORKOUT_DATE, 1, null);
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
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(1);

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.createWorkout(USER_A, WORKOUT_DATE, null, List.of(exerciseRequest))
		);

		verify(workoutRepository, never()).saveWorkout(USER_A, WORKOUT_DATE, 1, null);
	}

	@Test
	void createWorkoutFromOwnedRoutineCopiesRoutineDetailsWithWorkoutDefaults() {
		RoutineDetail routine = routineDetail(5);
		when(workoutRepository.findNextWorkoutOrder(USER_A, WORKOUT_DATE))
			.thenReturn(3);
		when(routineRepository.findDetailById(USER_A, 5))
			.thenReturn(Optional.of(routine));
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(workoutRepository.saveWorkout(USER_A, WORKOUT_DATE, 3, "오늘 기록"))
			.thenReturn(10);
		when(workoutRepository.saveWorkoutExercise(10, 1, "벤치프레스", ExerciseCategory.CHEST, 1, "벤치 메모", false))
			.thenReturn(20);

		WorkoutResponse workout = workoutService.createWorkoutFromRoutine(USER_A, 5, WORKOUT_DATE, "오늘 기록");

		assertEquals(10, workout.workoutId());
		assertEquals(3, workout.workoutOrder());
		assertEquals(false, workout.exercises().getFirst().completed());
		assertEquals(null, workout.exercises().getFirst().sets().getFirst().rpe());
		verify(workoutRepository).saveWorkout(USER_A, WORKOUT_DATE, 3, "오늘 기록");
		verify(workoutRepository).saveWorkoutSet(20, 1, BigDecimal.valueOf(80), 8, null, WorkoutSetType.WORKING, false);
	}

	@Test
	void createWorkoutFromRoutineRejectsOtherUserRoutine() {
		when(workoutRepository.findNextWorkoutOrder(USER_B, WORKOUT_DATE))
			.thenReturn(1);
		when(routineRepository.findDetailById(USER_B, 5))
			.thenReturn(Optional.empty());

		assertThrows(
			RoutineNotFoundException.class,
			() -> workoutService.createWorkoutFromRoutine(USER_B, 5, WORKOUT_DATE, null)
		);

		verify(workoutRepository, never()).saveWorkout(USER_B, WORKOUT_DATE, 1, null);
	}

	@Test
	void findWorkoutDatesUsesCurrentUserMonthRange() {
		List<LocalDate> dates = List.of(
			LocalDate.of(2026, 9, 1),
			LocalDate.of(2026, 9, 5)
		);
		when(workoutRepository.findWorkoutDates(
			USER_A,
			LocalDate.of(2026, 9, 1),
			LocalDate.of(2026, 10, 1)
		)).thenReturn(dates);

		assertEquals(dates, workoutService.findWorkoutDates(USER_A, 2026, 9));
		verify(workoutRepository).findWorkoutDates(USER_A, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
		verify(workoutRepository, never()).findWorkoutDates(USER_B, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
	}

	@Test
	void findWorkoutSummariesByDateReturnsCurrentUserResult() {
		List<WorkoutSummaryResponse> summaries = List.of(summary(10));
		when(workoutRepository.findSummariesByDate(USER_A, WORKOUT_DATE))
			.thenReturn(summaries);

		assertEquals(summaries, workoutService.findWorkoutSummariesByDate(USER_A, WORKOUT_DATE));
		verify(workoutRepository).findSummariesByDate(USER_A, WORKOUT_DATE);
	}

	@Test
	void findWorkoutSummariesByDateExcludesOtherUserResult() {
		when(workoutRepository.findSummariesByDate(USER_B, WORKOUT_DATE))
			.thenReturn(List.of());

		assertEquals(List.of(), workoutService.findWorkoutSummariesByDate(USER_B, WORKOUT_DATE));
		verify(workoutRepository).findSummariesByDate(USER_B, WORKOUT_DATE);
	}

	@Test
	void findWorkoutReturnsOwnedWorkout() {
		WorkoutResponse expected = workoutResponse(10);
		when(workoutRepository.findById(USER_A, 10))
			.thenReturn(Optional.of(expected));

		assertEquals(expected, workoutService.findWorkout(USER_A, 10));
	}

	@Test
	void findWorkoutRejectsOtherUserWorkout() {
		when(workoutRepository.findById(USER_B, 10))
			.thenReturn(Optional.empty());

		assertThrows(
			WorkoutNotFoundException.class,
			() -> workoutService.findWorkout(USER_B, 10)
		);
	}

	@Test
	void updateWorkoutKeepsExistingExerciseSnapshotAndReplacesSets() {
		WorkoutResponse current = workoutResponseWithExercise(10, WORKOUT_DATE, 1);
		WorkoutResponse updated = workoutResponseWithExercise(10, WORKOUT_DATE, 1);
		when(workoutRepository.findById(USER_A, 10))
			.thenReturn(Optional.of(current), Optional.of(updated));
		when(workoutRepository.updateWorkout(USER_A, 10, WORKOUT_DATE, 1, "수정 메모"))
			.thenReturn(1);
		when(workoutRepository.updateWorkoutExercise(10, 1, 1, "운동 메모", true))
			.thenReturn(Optional.of(20));

		WorkoutResponse result = workoutService.updateWorkout(
			USER_A,
			10,
			WORKOUT_DATE,
			"수정 메모",
			List.of(new WorkoutExerciseRequest(
				1,
				1,
				"운동 메모",
				true,
				List.of(set(1, 9, true), set(2, null, false))
			))
		);

		assertEquals(updated, result);
		verify(workoutRepository).updateWorkout(USER_A, 10, WORKOUT_DATE, 1, "수정 메모");
		verify(workoutRepository).updateWorkoutExercise(10, 1, 1, "운동 메모", true);
		verify(workoutRepository).deleteWorkoutSets(20);
		verify(workoutRepository).saveWorkoutSet(20, 1, BigDecimal.valueOf(80), 10, 9, WorkoutSetType.WORKING, true);
		verify(workoutRepository).saveWorkoutSet(20, 2, BigDecimal.valueOf(80), 10, null, WorkoutSetType.WORKING, false);
	}

	@Test
	void updateWorkoutRejectsExerciseChange() {
		when(workoutRepository.findById(USER_A, 10))
			.thenReturn(Optional.of(workoutResponseWithExercise(10, WORKOUT_DATE, 1)));

		assertThrows(
			WorkoutOperationException.class,
			() -> workoutService.updateWorkout(
				USER_A,
				10,
				WORKOUT_DATE,
				null,
				List.of(exercise(2, 1))
			)
		);

		verify(workoutRepository, never()).updateWorkout(USER_A, 10, WORKOUT_DATE, 1, null);
	}

	@Test
	void deleteWorkoutDeletesOwnedWorkout() {
		when(workoutRepository.deleteById(USER_A, 10))
			.thenReturn(1);

		workoutService.deleteWorkout(USER_A, 10);

		verify(workoutRepository).deleteById(USER_A, 10);
	}

	@Test
	void deleteWorkoutRejectsOtherUserWorkout() {
		when(workoutRepository.deleteById(USER_B, 10))
			.thenReturn(0);

		assertThrows(
			WorkoutNotFoundException.class,
			() -> workoutService.deleteWorkout(USER_B, 10)
		);
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

	private Exercise systemExercise(Integer exerciseId) {
		return new Exercise(
			exerciseId,
			"벤치프레스",
			ExerciseType.SYSTEM,
			ExerciseCategory.CHEST,
			true
		);
	}

	private WorkoutSummaryResponse summary(Integer workoutId) {
		return new WorkoutSummaryResponse(
			workoutId,
			WORKOUT_DATE,
			1,
			"가슴 운동",
			4,
			14
		);
	}

	private WorkoutResponse workoutResponse(Integer workoutId) {
		return new WorkoutResponse(
			workoutId,
			WORKOUT_DATE,
			1,
			"오늘 기록",
			List.of()
		);
	}

	private WorkoutResponse workoutResponseWithExercise(Integer workoutId, LocalDate workoutDate, Integer workoutOrder) {
		return new WorkoutResponse(
			workoutId,
			workoutDate,
			workoutOrder,
			"오늘 기록",
			List.of(new WorkoutExerciseResponse(
				1,
				"벤치프레스",
				ExerciseCategory.CHEST,
				1,
				null,
				false,
				List.of(new WorkoutSetResponse(
					1,
					BigDecimal.valueOf(80),
					10,
					null,
					WorkoutSetType.WORKING,
					false
				))
			))
		);
	}

	private RoutineDetail routineDetail(Integer routineId) {
		return new RoutineDetail(
			routineId,
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
	}
}
