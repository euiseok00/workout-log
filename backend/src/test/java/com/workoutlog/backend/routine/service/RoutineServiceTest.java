package com.workoutlog.backend.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.workoutlog.backend.routine.RoutineOperationException;
import com.workoutlog.backend.routine.RoutineSetDetail;
import com.workoutlog.backend.routine.RoutineSetType;
import com.workoutlog.backend.routine.RoutineSummary;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest.RoutineExerciseRequest;
import com.workoutlog.backend.routine.dto.RoutineCreateRequest.RoutineSetRequest;
import com.workoutlog.backend.routine.repository.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {
	private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private RoutineRepository routineRepository;

	@Mock
	private ExerciseRepository exerciseRepository;

	@InjectMocks
	private RoutineService routineService;

	@Test
	void createRoutineSavesUserIdExercisesAndSets() {
		RoutineSetRequest warmupSet = new RoutineSetRequest(
			1,
			BigDecimal.valueOf(20),
			15,
			RoutineSetType.WARMUP
		);
		RoutineSetRequest workingSet = new RoutineSetRequest(
			2,
			BigDecimal.valueOf(80),
			8,
			RoutineSetType.WORKING
		);
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(
			1,
			1,
			"벤치 메인",
			List.of(warmupSet, workingSet)
		);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(routineRepository.saveRoutine(USER_A, "Push A", "가슴 중심"))
			.thenReturn(10);
		when(routineRepository.saveRoutineExercise(10, 1, 1, "벤치 메인"))
			.thenReturn(20);

		RoutineSummary routine = routineService.createRoutine(
			USER_A,
			"Push A",
			"가슴 중심",
			List.of(routineExercise)
		);

		assertEquals(10, routine.id());
		assertEquals("Push A", routine.name());
		assertEquals(List.of("벤치프레스"), routine.exerciseNames());
		verify(routineRepository).saveRoutine(USER_A, "Push A", "가슴 중심");
		verify(routineRepository).saveRoutineSet(20, 1, BigDecimal.valueOf(20), 15, RoutineSetType.WARMUP);
		verify(routineRepository).saveRoutineSet(20, 2, BigDecimal.valueOf(80), 8, RoutineSetType.WORKING);
	}

	@Test
	void findRoutinesReturnsCurrentUserRoutines() {
		List<RoutineSummary> expected = List.of(new RoutineSummary(10, "A Routine", null, List.of()));
		when(routineRepository.findAll(USER_A))
			.thenReturn(expected);

		List<RoutineSummary> actual = routineService.findRoutines(USER_A);

		assertEquals(expected, actual);
		verify(routineRepository).findAll(USER_A);
		verify(routineRepository, never()).findAll(USER_B);
	}

	@Test
	void findRoutineDetailReturnsOwnedRoutine() {
		RoutineDetail expected = routineDetail(10);
		when(routineRepository.findDetailById(USER_A, 10))
			.thenReturn(Optional.of(expected));

		RoutineDetail actual = routineService.findRoutineDetail(USER_A, 10);

		assertEquals(expected, actual);
	}

	@Test
	void findRoutineDetailRejectsOtherUserRoutine() {
		when(routineRepository.findDetailById(USER_B, 10))
			.thenReturn(Optional.empty());

		assertThrows(
			RoutineNotFoundException.class,
			() -> routineService.findRoutineDetail(USER_B, 10)
		);
	}

	@Test
	void createRoutineAllowsSystemExercise() {
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(routineRepository.saveRoutine(USER_A, "Push A", null))
			.thenReturn(10);

		RoutineSummary routine = routineService.createRoutine(USER_A, "Push A", null, List.of(exercise(1, 1)));

		assertEquals(List.of("벤치프레스"), routine.exerciseNames());
	}

	@Test
	void createRoutineAllowsOwnedCustomExercise() {
		when(exerciseRepository.findAvailableById(USER_A, 2))
			.thenReturn(Optional.of(new Exercise(
				2,
				"My Row",
				ExerciseType.CUSTOM,
				ExerciseCategory.BACK,
				true
			)));
		when(routineRepository.saveRoutine(USER_A, "Pull A", null))
			.thenReturn(10);

		RoutineSummary routine = routineService.createRoutine(USER_A, "Pull A", null, List.of(exercise(2, 1)));

		assertEquals(List.of("My Row"), routine.exerciseNames());
	}

	@Test
	void createRoutineRejectsOtherUserCustomExercise() {
		RoutineExerciseRequest routineExercise = exercise(2, 1);
		when(exerciseRepository.findAvailableById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> routineService.createRoutine(USER_A, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine(USER_A, "Push A", null);
	}

	@Test
	void createRoutineRejectsDuplicateExerciseOrder() {
		RoutineExerciseRequest first = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		RoutineExerciseRequest second = new RoutineExerciseRequest(2, 1, null, List.of(set(1)));
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));

		assertThrows(
			RoutineOperationException.class,
			() -> routineService.createRoutine(USER_A, "Push A", null, List.of(first, second))
		);

		verify(routineRepository, never()).saveRoutine(USER_A, "Push A", null);
	}

	@Test
	void createRoutineRejectsDuplicateSetOrder() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(
			1,
			1,
			null,
			List.of(set(1), set(1))
		);

		assertThrows(
			RoutineOperationException.class,
			() -> routineService.createRoutine(USER_A, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine(USER_A, "Push A", null);
	}

	@Test
	void createRoutineRejectsInactiveExercise() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		assertThrows(
			RoutineOperationException.class,
			() -> routineService.createRoutine(USER_A, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine(USER_A, "Push A", null);
	}

	@Test
	void createRoutineRejectsUnknownExercise() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(99, 1, null, List.of(set(1)));
		when(exerciseRepository.findAvailableById(USER_A, 99))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> routineService.createRoutine(USER_A, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine(USER_A, "Push A", null);
	}

	@Test
	void updateRoutineUpdatesOwnedRoutineAndReplacesExercisesAndSets() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(
			1,
			1,
			"벤치 수정",
			List.of(set(1), set(2))
		);
		when(routineRepository.existsById(USER_A, 10))
			.thenReturn(true);
		when(exerciseRepository.findAvailableById(USER_A, 1))
			.thenReturn(Optional.of(systemExercise(1)));
		when(routineRepository.saveRoutineExercise(10, 1, 1, "벤치 수정"))
			.thenReturn(20);

		RoutineSummary routine = routineService.updateRoutine(
			USER_A,
			10,
			"수정된 Push A",
			"수정 메모",
			List.of(routineExercise)
		);

		assertEquals(10, routine.id());
		assertEquals("수정된 Push A", routine.name());
		assertEquals("수정 메모", routine.memo());
		verify(routineRepository).updateRoutine(USER_A, 10, "수정된 Push A", "수정 메모");
		verify(routineRepository).deleteRoutineExercisesByRoutineId(10);
		verify(routineRepository).saveRoutineSet(20, 1, BigDecimal.ZERO, 10, RoutineSetType.WORKING);
		verify(routineRepository).saveRoutineSet(20, 2, BigDecimal.ZERO, 10, RoutineSetType.WORKING);
	}

	@Test
	void updateRoutineRejectsOtherUserRoutine() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		when(routineRepository.existsById(USER_B, 10))
			.thenReturn(false);

		assertThrows(
			RoutineNotFoundException.class,
			() -> routineService.updateRoutine(USER_B, 10, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).updateRoutine(USER_B, 10, "Push A", null);
		verify(routineRepository, never()).deleteRoutineExercisesByRoutineId(10);
	}

	@Test
	void updateRoutineRejectsOtherUserCustomExercise() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(2, 1, null, List.of(set(1)));
		when(routineRepository.existsById(USER_A, 10))
			.thenReturn(true);
		when(exerciseRepository.findAvailableById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> routineService.updateRoutine(USER_A, 10, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).updateRoutine(USER_A, 10, "Push A", null);
		verify(routineRepository, never()).deleteRoutineExercisesByRoutineId(10);
	}

	@Test
	void updateRoutineRejectsUnknownRoutine() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		when(routineRepository.existsById(USER_A, 10))
			.thenReturn(false);

		assertThrows(
			RoutineNotFoundException.class,
			() -> routineService.updateRoutine(USER_A, 10, "Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).updateRoutine(USER_A, 10, "Push A", null);
	}

	@Test
	void deleteRoutineDeletesOwnedRoutine() {
		when(routineRepository.deleteById(USER_A, 10))
			.thenReturn(1);

		routineService.deleteRoutine(USER_A, 10);

		verify(routineRepository).deleteById(USER_A, 10);
	}

	@Test
	void deleteRoutineRejectsOtherUserRoutine() {
		when(routineRepository.deleteById(USER_B, 10))
			.thenReturn(0);

		assertThrows(
			RoutineNotFoundException.class,
			() -> routineService.deleteRoutine(USER_B, 10)
		);
	}

	private RoutineExerciseRequest exercise(Integer exerciseId, Integer exerciseOrder) {
		return new RoutineExerciseRequest(
			exerciseId,
			exerciseOrder,
			null,
			List.of(set(1))
		);
	}

	private RoutineSetRequest set(Integer setOrder) {
		return new RoutineSetRequest(
			setOrder,
			BigDecimal.ZERO,
			10,
			RoutineSetType.WORKING
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

	private RoutineDetail routineDetail(Integer routineId) {
		return new RoutineDetail(
			routineId,
			"Push A",
			null,
			List.of(new RoutineExerciseDetail(
				1,
				"벤치프레스",
				ExerciseCategory.CHEST,
				1,
				null,
				List.of(new RoutineSetDetail(
					1,
					BigDecimal.ZERO,
					10,
					RoutineSetType.WORKING
				))
			))
		);
	}
}
