package com.workoutlog.backend.routine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseType;
import com.workoutlog.backend.exercise.repository.ExerciseRepository;
import com.workoutlog.backend.routine.RoutineOperationException;
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
	@Mock
	private RoutineRepository routineRepository;

	@Mock
	private ExerciseRepository exerciseRepository;

	@InjectMocks
	private RoutineService routineService;

	@Test
	void createRoutineSavesExercisesAndSets() {
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
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.SYSTEM,
				ExerciseCategory.CHEST,
				true
			)));
		when(routineRepository.saveRoutine("Push A", "가슴 중심"))
			.thenReturn(10);
		when(routineRepository.saveRoutineExercise(10, 1, 1, "벤치 메인"))
			.thenReturn(20);

		RoutineSummary routine = routineService.createRoutine(
			"Push A",
			"가슴 중심",
			List.of(routineExercise)
		);

		assertEquals(10, routine.id());
		assertEquals("Push A", routine.name());
		assertEquals(List.of("벤치프레스"), routine.exerciseNames());
		verify(routineRepository).saveRoutineSet(20, 1, BigDecimal.valueOf(20), 15, RoutineSetType.WARMUP);
		verify(routineRepository).saveRoutineSet(20, 2, BigDecimal.valueOf(80), 8, RoutineSetType.WORKING);
	}

	@Test
	void createRoutineRejectsDuplicateExerciseOrder() {
		RoutineExerciseRequest first = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		RoutineExerciseRequest second = new RoutineExerciseRequest(2, 1, null, List.of(set(1)));
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.SYSTEM,
				ExerciseCategory.CHEST,
				true
			)));

		assertThrows(
			RoutineOperationException.class,
			() -> routineService.createRoutine("Push A", null, List.of(first, second))
		);

		verify(routineRepository, never()).saveRoutine("Push A", null);
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
			() -> routineService.createRoutine("Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine("Push A", null);
	}

	@Test
	void createRoutineRejectsInactiveExercise() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(1, 1, null, List.of(set(1)));
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"벤치프레스",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		assertThrows(
			RoutineOperationException.class,
			() -> routineService.createRoutine("Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine("Push A", null);
	}

	@Test
	void createRoutineRejectsUnknownExercise() {
		RoutineExerciseRequest routineExercise = new RoutineExerciseRequest(99, 1, null, List.of(set(1)));
		when(exerciseRepository.findById(99))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> routineService.createRoutine("Push A", null, List.of(routineExercise))
		);

		verify(routineRepository, never()).saveRoutine("Push A", null);
	}

	private RoutineSetRequest set(Integer setOrder) {
		return new RoutineSetRequest(
			setOrder,
			BigDecimal.ZERO,
			10,
			RoutineSetType.WORKING
		);
	}
}
