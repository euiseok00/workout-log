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
		when(exerciseRepository.findActive(USER_A, null))
			.thenReturn(expected);

		List<Exercise> actual = exerciseService.findActiveExercises(USER_A, null);

		assertEquals(expected, actual);
		verify(exerciseRepository).findActive(USER_A, null);
		verify(exerciseRepository, never()).findActive(USER_B, null);
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

		verify(exerciseRepository, never()).deactivate(USER_A, 1);
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

		verify(exerciseRepository).deactivate(USER_A, 1);
	}

	@Test
	void deactivateCustomExerciseRejectsOtherUserCustomExercise() {
		when(exerciseRepository.findCustomById(USER_A, 2))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.deactivateCustomExercise(USER_A, 2)
		);

		verify(exerciseRepository, never()).deactivate(USER_A, 2);
	}

	@Test
	void deactivateCustomExerciseRejectsSystemExercise() {
		when(exerciseRepository.findCustomById(USER_A, 1))
			.thenReturn(Optional.empty());

		assertThrows(
			ExerciseNotFoundException.class,
			() -> exerciseService.deactivateCustomExercise(USER_A, 1)
		);

		verify(exerciseRepository, never()).deactivate(USER_A, 1);
	}
}
