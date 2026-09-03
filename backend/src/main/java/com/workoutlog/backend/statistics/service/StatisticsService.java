package com.workoutlog.backend.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.statistics.dto.CategoryStatisticsPointResponse;
import com.workoutlog.backend.statistics.dto.ExerciseStatisticsDateResponse;
import com.workoutlog.backend.statistics.dto.ExerciseStatisticsDateResponse.SetResponse;
import com.workoutlog.backend.statistics.dto.ExerciseStatisticsDateResponse.WorkoutResponse;
import com.workoutlog.backend.statistics.repository.StatisticsRepository;
import com.workoutlog.backend.statistics.repository.StatisticsRepository.ExerciseStatisticsSetRow;
import com.workoutlog.backend.workout.WorkoutOperationException;
import com.workoutlog.backend.workout.WorkoutSetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private static final EnumSet<WorkoutSetType> VOLUME_SET_TYPES = EnumSet.of(
		WorkoutSetType.WORKING,
		WorkoutSetType.TOP,
		WorkoutSetType.FAILURE,
		WorkoutSetType.BACKOFF,
		WorkoutSetType.DROP
	);

	private final StatisticsRepository statisticsRepository;

	public StatisticsService(StatisticsRepository statisticsRepository) {
		this.statisticsRepository = statisticsRepository;
	}

	@Transactional(readOnly = true)
	public List<ExerciseStatisticsDateResponse> findExerciseStatistics(
		UUID userId,
		Integer exerciseId,
		LocalDate fromDate,
		LocalDate toDate
	) {
		validateDateRange(fromDate, toDate);

		List<ExerciseStatisticsSetRow> rows = statisticsRepository.findExerciseSetRows(
			userId,
			exerciseId,
			fromDate,
			toDate
		);
		Map<LocalDate, DayBuilder> days = new LinkedHashMap<>();

		for (ExerciseStatisticsSetRow row : rows) {
			days.computeIfAbsent(row.date(), DayBuilder::new).add(row);
		}

		return days.values()
			.stream()
			.map(DayBuilder::build)
			.filter(day -> day.volume().compareTo(BigDecimal.ZERO) > 0)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<CategoryStatisticsPointResponse> findCategoryStatistics(
		UUID userId,
		ExerciseCategory category,
		LocalDate fromDate,
		LocalDate toDate
	) {
		validateDateRange(fromDate, toDate);
		return statisticsRepository.findCategoryVolumes(userId, category, fromDate, toDate);
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && fromDate.isAfter(toDate)) {
			throw new WorkoutOperationException("from cannot be after to.");
		}
	}

	private static boolean isVolumeSet(ExerciseStatisticsSetRow row) {
		return row.completed()
			&& row.weight() != null
			&& row.weight().compareTo(BigDecimal.ZERO) > 0
			&& VOLUME_SET_TYPES.contains(row.setType());
	}

	private static class DayBuilder {
		private final LocalDate date;
		private final Map<Integer, WorkoutBuilder> workouts = new LinkedHashMap<>();
		private BigDecimal volume = BigDecimal.ZERO;

		DayBuilder(LocalDate date) {
			this.date = date;
		}

		void add(ExerciseStatisticsSetRow row) {
			workouts.computeIfAbsent(row.workoutId(), key -> new WorkoutBuilder(row.workoutId(), row.workoutOrder()))
				.add(row);

			if (isVolumeSet(row)) {
				volume = volume.add(row.weight().multiply(BigDecimal.valueOf(row.reps())));
			}
		}

		ExerciseStatisticsDateResponse build() {
			return new ExerciseStatisticsDateResponse(
				date,
				volume,
				workouts.size(),
				workouts.values().stream().map(WorkoutBuilder::build).toList()
			);
		}
	}

	private static class WorkoutBuilder {
		private final Integer workoutId;
		private final Integer workoutOrder;
		private final List<SetResponse> sets = new ArrayList<>();

		WorkoutBuilder(Integer workoutId, Integer workoutOrder) {
			this.workoutId = workoutId;
			this.workoutOrder = workoutOrder;
		}

		void add(ExerciseStatisticsSetRow row) {
			sets.add(new SetResponse(
				row.setOrder(),
				row.weight(),
				row.reps(),
				row.setType(),
				row.completed()
			));
		}

		WorkoutResponse build() {
			return new WorkoutResponse(workoutId, workoutOrder, sets);
		}
	}
}
