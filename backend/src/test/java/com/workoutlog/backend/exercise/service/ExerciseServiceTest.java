package com.workoutlog.backend.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseNotFoundException;
import com.workoutlog.backend.exercise.ExerciseOperationException;
import com.workoutlog.backend.exercise.ExerciseType;
import com.workoutlog.backend.exercise.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {
	private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private ExerciseRepository exerciseRepository;

	@InjectMocks
	private ExerciseService exerciseService;

	@Test
	void findActiveExercisesReturnsSystemAndCurrentUserCustomExercises() {
		List<Exercise> expected = List.of(
			new Exercise(1, "Squat", ExerciseType.SYSTEM, ExerciseCategory.LEGS, true),
			new Exercise(2, "My Row", ExerciseType.CUSTOM, ExerciseCategory.BACK, true)
		);
		when(exerciseRepository.findByFilters(USER_A, null, true))
			.thenReturn(expected);

		List<Exercise> actual = exerciseService.findExercises(USER_A, null, true);

		assertEquals(expected, actual);
		verify(exerciseRepository).findByFilters(USER_A, null, true);
		verify(exerciseRepository, never()).findByFilters(USER_B, null, true);
	}

	@Test
	void createCustomExerciseSavesCustomExercise() {
		Exercise expected = new Exercise(
			1,
			"Bench Press",
			ExerciseType.CUSTOM,
			ExerciseCategory.CHEST,
			true
		);
		when(exerciseRepository.saveCustom(USER_A, "Bench Press", ExerciseCategory.CHEST))
			.thenReturn(expected);

		Exercise actual = exerciseService.createCustomExercise(USER_A, "Bench Press", ExerciseCategory.CHEST);

		assertEquals(expected, actual);
	}

	@Test
	void updateCustomExerciseUpdatesOwnedCustomExercise() {
		Exercise ownedCustom = new Exercise(
			1,
			"Bench Press",
			ExerciseType.CUSTOM,
			ExerciseCategory.CHEST,
			true
		);
		Exercise expected = new Exercise(
			1,
			"New Name",
			ExerciseType.CUSTOM,
			ExerciseCategory.BACK,
			true
		);
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(ownedCustom));
		when(exerciseRepository.update(USER_A, 1, "New Name", ExerciseCategory.BACK))
			.thenReturn(expected);

		Exercise actual = exerciseService.updateCustomExercise(USER_A, 1, "New Name", ExerciseCategory.BACK);

		assertEquals(expected, actual);
	}

	@Test
	void updateCustomExerciseRejectsOtherUserCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.updateCustomExercise(USER_A, 2, "New Name", ExerciseCategory.BACK)
		);

		verify(exerciseRepository, never()).update(USER_A, 2, "New Name", ExerciseCategory.BACK);
	}

	@Test
	void updateCustomExerciseRejectsSystemExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.updateCustomExercise(USER_A, 1, "New Name", ExerciseCategory.BACK)
		);
	}

	@Test
	void updateCustomExerciseRejectsInactiveExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		assertThrows(
			ExerciseOperationException.class,
			() -> exerciseService.updateCustomExercise(USER_A, 1, "New Name", ExerciseCategory.BACK)
		);

		verify(exerciseRepository, never()).update(USER_A, 1, "New Name", ExerciseCategory.BACK);
	}

	@Test
	void deactivateCustomExerciseDoesNothingWhenAlreadyInactive() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		exerciseService.deactivateCustomExercise(USER_A, 1);

		verify(exerciseRepository, never()).setActive(USER_A, 1, false);
	}

	@Test
	void deactivateCustomExerciseDeactivatesOwnedCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				true
			)));

		exerciseService.deactivateCustomExercise(USER_A, 1);

		verify(exerciseRepository).setActive(USER_A, 1, false);
	}

	@Test
	void deactivateCustomExerciseRejectsOtherUserCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.deactivateCustomExercise(USER_A, 2)
		);

		verify(exerciseRepository, never()).setActive(USER_A, 2, false);
	}

	@Test
	void deactivateCustomExerciseRejectsSystemExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.deactivateCustomExercise(USER_A, 1)
		);

		verify(exerciseRepository, never()).setActive(USER_A, 1, false);
	}

	@Test
	void activateCustomExerciseDoesNothingWhenAlreadyActive() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				true
			)));

		exerciseService.activateCustomExercise(USER_A, 1);

		verify(exerciseRepository, never()).setActive(USER_A, 1, true);
	}

	@Test
	void activateCustomExerciseActivatesOwnedCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		exerciseService.activateCustomExercise(USER_A, 1);

		verify(exerciseRepository).setActive(USER_A, 1, true);
	}

	@Test
	void activateCustomExerciseRejectsOtherUserCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.activateCustomExercise(USER_A, 2)
		);

		verify(exerciseRepository, never()).setActive(USER_A, 2, true);
	}
}
