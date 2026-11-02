package com.workoutlog.backend.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.routine.Routine;
import com.workoutlog.backend.routine.RoutineNotFoundException;
import com.workoutlog.backend.routine.RoutineRepository;
import com.workoutlog.backend.statistics.dto.StatisticsResponse;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.exercise.ExerciseLookup;
import com.workoutlog.backend.statistics.dto.StatisticsResponse.History;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;
import com.workoutlog.backend.workout.WorkoutSession;
import com.workoutlog.backend.workout.WorkoutSessionRepository;

@Service
public class StatisticsService {

	private static final List<BodyPart> BODY_PARTS = List.of(BodyPart.CHEST, BodyPart.BACK, BodyPart.SHOULDERS,
			BodyPart.ARMS, BodyPart.LEGS);
	private static final int MAX_PERIOD_DAYS = 365;

	private final WorkoutSessionRepository workoutSessionRepository;
	private final ExerciseLookup exerciseLookup;
	private final RoutineRepository routineRepository;

	public StatisticsService(WorkoutSessionRepository workoutSessionRepository,
			ExerciseLookup exerciseLookup, RoutineRepository routineRepository) {
		this.workoutSessionRepository = workoutSessionRepository;
		this.exerciseLookup = exerciseLookup;
		this.routineRepository = routineRepository;
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Summary getSummary(Integer userId, LocalDate from, LocalDate to) {
		List<WorkoutSession> sessions = sessions(userId, from, to);
		Totals totals = new Totals();
		for (WorkoutSession session : sessions) {
			totals.addSession(session.getExercises());
		}
		return new StatisticsResponse.Summary(sessions.size(), totals.setCount, totals.reps, totals.volume);
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Dashboard getDashboard(Integer userId, LocalDate today) {
		LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate previousMonday = currentMonday.minusWeeks(1);
		LocalDate currentSunday = currentMonday.plusDays(6);
		LocalDate monthStart = today.withDayOfMonth(1);
		LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
		LocalDate from = monthStart.isBefore(previousMonday) ? monthStart : previousMonday;
		LocalDate to = monthEnd.isAfter(currentSunday) ? monthEnd : currentSunday;
		HashSet<LocalDate> workoutDays = new HashSet<>();
		Totals current = new Totals();
		Totals previous = new Totals();
		Map<BodyPart, Totals> currentParts = new EnumMap<>(BodyPart.class);
		Map<BodyPart, Totals> previousParts = new EnumMap<>(BodyPart.class);

		for (WorkoutSession session : sessions(userId, from, to)) {
			LocalDate date = session.getWorkoutDate();
			if (!date.isBefore(monthStart) && !date.isAfter(monthEnd)) workoutDays.add(date);
			if (date.isBefore(previousMonday) || date.isAfter(currentSunday)) continue;
			Totals week = date.isBefore(currentMonday) ? previous : current;
			Map<BodyPart, Totals> parts = date.isBefore(currentMonday) ? previousParts : currentParts;
			week.addSession(session.getExercises());
			for (ExerciseInSession exercise : session.getExercises()) {
				parts.computeIfAbsent(exercise.getExercise().getBodyPart(), ignored -> new Totals())
						.add(exercise.getSets());
			}
		}

		List<StatisticsResponse.BodyPartVolume> bodyPartVolumes = BODY_PARTS.stream()
				.map(part -> new StatisticsResponse.BodyPartVolume(part,
						currentParts.getOrDefault(part, new Totals()).volume,
						previousParts.getOrDefault(part, new Totals()).volume))
				.toList();
		StatisticsResponse.RecentSession recent = workoutSessionRepository
				.findFirstByUserIdOrderByWorkoutDateDescIdDesc(userId)
				.map(session -> new StatisticsResponse.RecentSession(session.getId(), session.getWorkoutDate(),
						session.getExercises().size()))
				.orElse(null);
		return new StatisticsResponse.Dashboard(workoutDays.size(), current.setCount, previous.setCount,
				current.volume, previous.volume, bodyPartVolumes, recent);
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Exercise getExerciseStatistics(Integer exerciseId, Integer userId, LocalDate from,
			LocalDate to) {
		Exercise exercise = exerciseLookup.findVisible(exerciseId, userId);
		Map<LocalDate, Totals> history = new LinkedHashMap<>();
		long sessionCount = 0;

		for (WorkoutSession session : sessions(userId, from, to)) {
			List<ExerciseInSession> matches = session.getExercises().stream()
					.filter(value -> value.getExercise().getId().equals(exerciseId))
					.toList();
			if (!matches.isEmpty()) {
				sessionCount++;
				Totals daily = history.computeIfAbsent(session.getWorkoutDate(), ignored -> new Totals());
				matches.forEach(value -> daily.add(value.getSets()));
			}
		}

		Totals total = new Totals();
		List<History> historyResponse = new ArrayList<>();
		for (Map.Entry<LocalDate, Totals> entry : history.entrySet()) {
			Totals daily = entry.getValue();
			total.add(daily);
			historyResponse.add(new History(entry.getKey(), daily.setCount, daily.reps, daily.volume, daily.maxWeight));
		}
		return new StatisticsResponse.Exercise(exercise.getId(), exercise.getName(), exercise.getBodyPart(),
				sessionCount, total.setCount, total.reps, total.volume, total.maxWeight, historyResponse);
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.VolumeComparison getVolumeComparison(Integer userId, LocalDate from, int days) {
		if (days < 1 || days > MAX_PERIOD_DAYS) {
			throw new RequestValidationException("days", "기간은 1일 이상 365일 이하여야 합니다.");
		}
		LocalDate previousFrom = from.minusDays(days);
		LocalDate nextFrom = from.plusDays(days);
		List<WorkoutSession> sessions = sessions(userId, previousFrom, nextFrom.plusDays(days - 1L));
		return new StatisticsResponse.VolumeComparison(days, volumePeriod(sessions, previousFrom, days),
				volumePeriod(sessions, from, days), volumePeriod(sessions, nextFrom, days));
	}

	private StatisticsResponse.VolumePeriod volumePeriod(List<WorkoutSession> sessions, LocalDate from, int days) {
		LocalDate to = from.plusDays(days - 1L);
		Totals total = new Totals();
		Map<BodyPart, Totals> parts = new EnumMap<>(BodyPart.class);
		for (WorkoutSession session : sessions) {
			if (session.getWorkoutDate().isBefore(from) || session.getWorkoutDate().isAfter(to)) continue;
			for (ExerciseInSession exercise : session.getExercises()) {
				total.add(exercise.getSets());
				parts.computeIfAbsent(exercise.getExercise().getBodyPart(), ignored -> new Totals())
						.add(exercise.getSets());
			}
		}
		return new StatisticsResponse.VolumePeriod(from, to, total.volume, BODY_PARTS.stream()
				.map(part -> new StatisticsResponse.PartVolume(part, parts.getOrDefault(part, new Totals()).volume))
				.toList());
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Routine getRoutineStatistics(Integer routineId, Integer userId, LocalDate from,
			LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new RequestValidationException("from", "시작일은 종료일보다 이후일 수 없습니다.");
		}
		Routine routine = routineRepository.findByIdAndUserId(routineId, userId)
				.orElseThrow(RoutineNotFoundException::new);
		Totals total = new Totals();
		List<StatisticsResponse.RoutineSession> sessions = new ArrayList<>();
		for (WorkoutSession session : workoutSessionRepository.findAllByRoutineInDateRange(userId, routineId, from, to)) {
			Totals sessionTotal = new Totals();
			sessionTotal.addSession(session.getExercises());
			List<StatisticsResponse.RoutineSessionExercise> exercises = new ArrayList<>();
			for (ExerciseInSession exercise : session.getExercises()) {
				Totals exerciseTotal = new Totals();
				exerciseTotal.add(exercise.getSets());
				exercises.add(new StatisticsResponse.RoutineSessionExercise(exercise.getExercise().getId(),
						exercise.getExercise().getName(), exercise.getExercise().getBodyPart(),
						exerciseTotal.setCount, exerciseTotal.volume, exerciseTotal.maxWeight));
			}
			total.add(sessionTotal);
			sessions.add(new StatisticsResponse.RoutineSession(session.getId(), session.getWorkoutDate(),
					sessionTotal.setCount, sessionTotal.reps, sessionTotal.volume, sessionTotal.maxWeight, exercises));
		}
		return new StatisticsResponse.Routine(routine.getId(), routine.getName(), sessions.size(), total.setCount,
				total.reps, total.volume, total.maxWeight, sessions);
	}

	private List<WorkoutSession> sessions(Integer userId, LocalDate from, LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new RequestValidationException("from", "시작일은 종료일보다 이후일 수 없습니다.");
		}
		return workoutSessionRepository.findAllInDateRange(userId, from, to);
	}

	private static final class Totals {
		private long setCount;
		private long reps;
		private BigDecimal volume = BigDecimal.ZERO;
		private BigDecimal maxWeight = BigDecimal.ZERO;

		private void add(List<ExerciseSet> sets) {
			for (ExerciseSet set : sets) {
				if (set.isCompleted() && set.getSetType().countsTowardVolume()) {
					if (set.getSetType().countsAsSet()) setCount++;
					reps += set.getReps();
					volume = volume.add(set.getWeight().multiply(BigDecimal.valueOf(set.getReps())));
					maxWeight = maxWeight.max(set.getWeight());
				}
			}
		}

		// 슈퍼·트라이·자이언트는 묶음의 한 라운드를 한 세트로 센다. 라운드 수는 가장 많이 완료한 운동 기준이다.
		private void addSession(List<ExerciseInSession> exercises) {
			Map<Integer, Long> groupRounds = new HashMap<>();
			for (ExerciseInSession exercise : exercises) {
				add(exercise.getSets());
				if (exercise.getGroupNumber() == null) continue;
				long rounds = exercise.getSets().stream()
						.filter(set -> set.isCompleted() && set.getSetType().requiresGroup())
						.count();
				setCount -= rounds;
				groupRounds.merge(exercise.getGroupNumber(), rounds, Math::max);
			}
			setCount += groupRounds.values().stream().mapToLong(Long::longValue).sum();
		}

		private void add(Totals other) {
			setCount += other.setCount;
			reps += other.reps;
			volume = volume.add(other.volume);
			maxWeight = maxWeight.max(other.maxWeight);
		}
	}
}
