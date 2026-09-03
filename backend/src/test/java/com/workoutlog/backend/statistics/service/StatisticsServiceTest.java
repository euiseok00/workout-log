package com.workoutlog.backend.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.statistics.dto.CategoryStatisticsPointResponse;
import com.workoutlog.backend.statistics.dto.ExerciseStatisticsDateResponse;
import com.workoutlog.backend.statistics.repository.StatisticsRepository;
import com.workoutlog.backend.statistics.repository.StatisticsRepository.ExerciseStatisticsSetRow;
import com.workoutlog.backend.workout.WorkoutOperationException;
import com.workoutlog.backend.workout.WorkoutSetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final LocalDate FROM_DATE = LocalDate.of(2026, 6, 3);
	private static final LocalDate TO_DATE = LocalDate.of(2026, 9, 3);

	@Mock
	private StatisticsRepository statisticsRepository;

	@InjectMocks
	private StatisticsService statisticsService;

	@Test
	void findExerciseStatisticsGroupsByDateAndWorkoutAndUsesOnlyVolumeSets() {
		when(statisticsRepository.findExerciseSetRows(USER_ID, 1, FROM_DATE, TO_DATE))
			.thenReturn(List.of(
				row(LocalDate.of(2026, 9, 1), 10, 1, 1, 20, 10, WorkoutSetType.WARMUP, true),
				row(LocalDate.of(2026, 9, 1), 10, 1, 2, 80, 5, WorkoutSetType.WORKING, true),
				row(LocalDate.of(2026, 9, 1), 11, 2, 1, 90, 3, WorkoutSetType.BACKOFF, true),
				row(LocalDate.of(2026, 9, 2), 12, 1, 1, 100, 2, WorkoutSetType.TOP, false)
			));

		List<ExerciseStatisticsDateResponse> result = statisticsService.findExerciseStatistics(
			USER_ID,
			1,
			FROM_DATE,
			TO_DATE
		);

		assertEquals(1, result.size());
		assertEquals(LocalDate.of(2026, 9, 1), result.getFirst().date());
		assertEquals(BigDecimal.valueOf(670), result.getFirst().volume());
		assertEquals(2, result.getFirst().workoutCount());
		assertEquals(2, result.getFirst().workouts().size());
		assertEquals(2, result.getFirst().workouts().getFirst().sets().size());
	}

	@Test
	void findCategoryStatisticsDelegatesSnapshotCategoryQuery() {
		List<CategoryStatisticsPointResponse> points = List.of(
			new CategoryStatisticsPointResponse(LocalDate.of(2026, 9, 1), BigDecimal.valueOf(400))
		);
		when(statisticsRepository.findCategoryVolumes(USER_ID, ExerciseCategory.BACK, null, TO_DATE))
			.thenReturn(points);

		assertEquals(points, statisticsService.findCategoryStatistics(USER_ID, ExerciseCategory.BACK, null, TO_DATE));
	}

	@Test
	void rejectsInvalidDateRange() {
		assertThrows(
			WorkoutOperationException.class,
			() -> statisticsService.findExerciseStatistics(USER_ID, 1, TO_DATE, FROM_DATE)
		);
	}

	private ExerciseStatisticsSetRow row(
		LocalDate date,
		Integer workoutId,
		Integer workoutOrder,
		Integer setOrder,
		int weight,
		int reps,
		WorkoutSetType setType,
		boolean completed
	) {
		return new ExerciseStatisticsSetRow(
			date,
			workoutId,
			workoutOrder,
			setOrder,
			BigDecimal.valueOf(weight),
			reps,
			setType,
			completed
		);
	}
}
