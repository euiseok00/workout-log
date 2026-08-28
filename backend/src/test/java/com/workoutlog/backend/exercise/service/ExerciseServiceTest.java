package com.workoutlog.backend.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
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
	@Mock
	private ExerciseRepository exerciseRepository;

	@InjectMocks
	private ExerciseService exerciseService;

	@Test
	void createCustomExerciseSavesCustomExercise() {
		Exercise expected = new Exercise(
			1,
			"Bench Press",
			ExerciseType.CUSTOM,
			ExerciseCategory.CHEST,
			true
		);
		when(exerciseRepository.saveCustom("Bench Press", ExerciseCategory.CHEST))
			.thenReturn(expected);

		Exercise actual = exerciseService.createCustomExercise("Bench Press", ExerciseCategory.CHEST);

		assertEquals(expected, actual);
	}

	@Test
	void updateCustomExerciseRejectsSystemExercise() {
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.SYSTEM,
				ExerciseCategory.CHEST,
				true
			)));

		assertThrows(
			ExerciseOperationException.class,
			() -> exerciseService.updateCustomExercise(1, "New Name", ExerciseCategory.BACK)
		);
	}

	@Test
	void deactivateCustomExerciseDoesNothingWhenAlreadyInactive() {
		when(exerciseRepository.findById(1))
			.thenReturn(Optional.of(new Exercise(
				1,
				"Bench Press",
				ExerciseType.CUSTOM,
				ExerciseCategory.CHEST,
				false
			)));

		exerciseService.deactivateCustomExercise(1);

		verify(exerciseRepository, never()).deactivate(1);
	}
}
