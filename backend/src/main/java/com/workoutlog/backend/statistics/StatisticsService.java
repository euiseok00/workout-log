package com.workoutlog.backend.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.statistics.StatisticsResponse.History;
import com.workoutlog.backend.workout.ExerciseInSession;
import com.workoutlog.backend.workout.ExerciseSet;
import com.workoutlog.backend.workout.WorkoutSession;
import com.workoutlog.backend.workout.WorkoutSessionRepository;

@Service
public class StatisticsService {

	private final WorkoutSessionRepository workoutSessionRepository;
	private final ExerciseRepository exerciseRepository;

	public StatisticsService(WorkoutSessionRepository workoutSessionRepository,
			ExerciseRepository exerciseRepository) {
		this.workoutSessionRepository = workoutSessionRepository;
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Summary getSummary(Integer userId, LocalDate from, LocalDate to) {
		List<WorkoutSession> sessions = sessions(userId, from, to);
		Totals totals = new Totals();
		for (WorkoutSession session : sessions) {
			for (ExerciseInSession exercise : session.getExercises()) {
				totals.add(exercise.getSets());
			}
		}
		return new StatisticsResponse.Summary(sessions.size(), totals.setCount, totals.reps, totals.volume);
	}

	@Transactional(readOnly = true)
	public StatisticsResponse.Exercise getExerciseStatistics(Integer exerciseId, Integer userId, LocalDate from,
			LocalDate to) {
		Exercise exercise = exerciseRepository.findById(exerciseId)
				.filter(value -> value.getUserId() == null || value.getUserId().equals(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
				if (set.isCompleted()) {
					setCount++;
					reps += set.getReps();
					volume = volume.add(set.getWeight().multiply(BigDecimal.valueOf(set.getReps())));
					maxWeight = maxWeight.max(set.getWeight());
				}
			}
		}

		private void add(Totals other) {
			setCount += other.setCount;
			reps += other.reps;
			volume = volume.add(other.volume);
			maxWeight = maxWeight.max(other.maxWeight);
		}
	}
}
