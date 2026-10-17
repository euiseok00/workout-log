package com.workoutlog.backend.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.workoutlog.backend.exercise.BodyPart;

public final class StatisticsResponse {

	private StatisticsResponse() {
	}

	public record Summary(long workoutCount, long completedSetCount, long totalReps, BigDecimal totalVolume) {
	}

	public record Dashboard(long monthlyWorkoutDays, long currentWeekCompletedSetCount,
			long previousWeekCompletedSetCount, BigDecimal currentWeekVolume, BigDecimal previousWeekVolume,
			List<BodyPartVolume> bodyPartVolumes, RecentSession recentSession) {
	}

	public record BodyPartVolume(BodyPart bodyPart, BigDecimal currentWeekVolume, BigDecimal previousWeekVolume) {
	}

	public record RecentSession(Integer id, LocalDate workoutDate, int exerciseCount) {
	}

	public record Exercise(Integer exerciseId, String name, BodyPart bodyPart, long sessionCount,
			long completedSetCount, long totalReps, BigDecimal totalVolume, BigDecimal maxWeight,
			List<History> history) {
	}

	public record History(LocalDate workoutDate, long completedSetCount, long totalReps, BigDecimal totalVolume,
			BigDecimal maxWeight) {
	}
}
