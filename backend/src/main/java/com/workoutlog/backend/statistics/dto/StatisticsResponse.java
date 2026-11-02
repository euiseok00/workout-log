package com.workoutlog.backend.statistics.dto;

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

	public record VolumeComparison(int days, VolumePeriod previous, VolumePeriod selected, VolumePeriod next) {
	}

	public record VolumePeriod(LocalDate from, LocalDate to, BigDecimal totalVolume, List<PartVolume> bodyParts) {
	}

	public record PartVolume(BodyPart bodyPart, BigDecimal volume) {
	}

	public record Routine(Integer routineId, String name, long sessionCount, long completedSetCount, long totalReps,
			BigDecimal totalVolume, BigDecimal maxWeight, List<RoutineSession> sessions) {
	}

	public record RoutineSession(Integer sessionId, LocalDate workoutDate, long completedSetCount, long totalReps,
			BigDecimal totalVolume, BigDecimal maxWeight, List<RoutineSessionExercise> exercises) {
	}

	public record RoutineSessionExercise(Integer exerciseId, String name, BodyPart bodyPart, long completedSetCount,
			BigDecimal totalVolume, BigDecimal maxWeight) {
	}

	public record History(LocalDate workoutDate, long completedSetCount, long totalReps, BigDecimal totalVolume,
			BigDecimal maxWeight) {
	}
}
