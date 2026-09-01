package com.workoutlog.backend.workout.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.workoutlog.backend.workout.WorkoutExerciseResponse;
import com.workoutlog.backend.workout.WorkoutResponse;
import com.workoutlog.backend.workout.WorkoutSetResponse;
import com.workoutlog.backend.workout.WorkoutSetType;
import com.workoutlog.backend.workout.WorkoutSummaryResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutRepository {
	private final JdbcClient jdbcClient;

	public WorkoutRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public Integer findNextWorkoutOrder(LocalDate workoutDate) {
		return jdbcClient.sql("""
				SELECT COALESCE(MAX(workout_order), 0) + 1
				FROM workouts
				WHERE workout_date = :workoutDate
				""")
			.param("workoutDate", workoutDate)
			.query(Integer.class)
			.single();
	}

	public Optional<WorkoutResponse> findById(Integer workoutId) {
		Optional<WorkoutHeader> header = findHeaderById(workoutId);
		if (header.isEmpty()) {
			return Optional.empty();
		}

		List<WorkoutDetailRow> rows = jdbcClient.sql("""
				SELECT we.workout_exercise_id,
				       we.exercise_id,
				       we.exercise_name,
				       we.exercise_order,
				       we.memo AS exercise_memo,
				       we.completed AS exercise_completed,
				       ws.set_order,
				       ws.weight,
				       ws.reps,
				       ws.rpe,
				       ws.set_type,
				       ws.completed AS set_completed
				FROM workout_exercises we
				LEFT JOIN workout_sets ws ON we.workout_exercise_id = ws.workout_exercise_id
				WHERE we.workout_id = :workoutId
				ORDER BY we.exercise_order, ws.set_order
				""")
			.param("workoutId", workoutId)
			.query((rs, rowNum) -> new WorkoutDetailRow(
				rs.getInt("workout_exercise_id"),
				rs.getInt("exercise_id"),
				rs.getString("exercise_name"),
				rs.getInt("exercise_order"),
				rs.getString("exercise_memo"),
				rs.getBoolean("exercise_completed"),
				(Integer)rs.getObject("set_order"),
				rs.getBigDecimal("weight"),
				(Integer)rs.getObject("reps"),
				(Integer)rs.getObject("rpe"),
				rs.getString("set_type") == null ? null : WorkoutSetType.valueOf(rs.getString("set_type")),
				rs.getBoolean("set_completed")
			))
			.list();

		Map<Integer, WorkoutExerciseBuilder> exercises = new LinkedHashMap<>();
		for (WorkoutDetailRow row : rows) {
			WorkoutExerciseBuilder exercise = exercises.computeIfAbsent(
				row.workoutExerciseId(),
				key -> new WorkoutExerciseBuilder(row)
			);

			if (row.setOrder() != null) {
				exercise.addSet(new WorkoutSetResponse(
					row.setOrder(),
					row.weight(),
					row.reps(),
					row.rpe(),
					row.setType(),
					row.setCompleted()
				));
			}
		}

		WorkoutHeader workout = header.get();
		return Optional.of(new WorkoutResponse(
			workout.workoutId(),
			workout.workoutDate(),
			workout.workoutOrder(),
			workout.memo(),
			exercises.values()
				.stream()
				.map(WorkoutExerciseBuilder::build)
				.toList()
		));
	}

	public List<LocalDate> findWorkoutDates(LocalDate startDate, LocalDate nextMonthStartDate) {
		return jdbcClient.sql("""
				SELECT DISTINCT workout_date
				FROM workouts
				WHERE workout_date >= :startDate
				  AND workout_date < :nextMonthStartDate
				ORDER BY workout_date
				""")
			.param("startDate", startDate)
			.param("nextMonthStartDate", nextMonthStartDate)
			.query(LocalDate.class)
			.list();
	}

	public List<WorkoutSummaryResponse> findSummariesByDate(LocalDate date) {
		return jdbcClient.sql("""
				SELECT w.workout_id,
				       w.workout_date,
				       w.workout_order,
				       w.memo,
				       COUNT(DISTINCT we.workout_exercise_id) AS exercise_count,
				       COUNT(ws.workout_set_id) AS set_count
				FROM workouts w
				LEFT JOIN workout_exercises we ON w.workout_id = we.workout_id
				LEFT JOIN workout_sets ws ON we.workout_exercise_id = ws.workout_exercise_id
				WHERE w.workout_date = :date
				GROUP BY w.workout_id, w.workout_date, w.workout_order, w.memo
				ORDER BY w.workout_order
				""")
			.param("date", date)
			.query((rs, rowNum) -> new WorkoutSummaryResponse(
				rs.getInt("workout_id"),
				rs.getObject("workout_date", LocalDate.class),
				rs.getInt("workout_order"),
				rs.getString("memo"),
				rs.getInt("exercise_count"),
				rs.getInt("set_count")
			))
			.list();
	}

	public Integer saveWorkout(LocalDate workoutDate, Integer workoutOrder, String memo) {
		return jdbcClient.sql("""
				INSERT INTO workouts (workout_date, workout_order, memo)
				VALUES (:workoutDate, :workoutOrder, :memo)
				RETURNING workout_id
				""")
			.param("workoutDate", workoutDate)
			.param("workoutOrder", workoutOrder)
			.param("memo", memo)
			.query(Integer.class)
			.single();
	}

	public Integer saveWorkoutExercise(
		Integer workoutId,
		Integer exerciseId,
		String exerciseName,
		Integer exerciseOrder,
		String memo,
		boolean completed
	) {
		return jdbcClient.sql("""
				INSERT INTO workout_exercises (
				    workout_id,
				    exercise_id,
				    exercise_name,
				    exercise_order,
				    memo,
				    completed
				)
				VALUES (
				    :workoutId,
				    :exerciseId,
				    :exerciseName,
				    :exerciseOrder,
				    :memo,
				    :completed
				)
				RETURNING workout_exercise_id
				""")
			.param("workoutId", workoutId)
			.param("exerciseId", exerciseId)
			.param("exerciseName", exerciseName)
			.param("exerciseOrder", exerciseOrder)
			.param("memo", memo)
			.param("completed", completed)
			.query(Integer.class)
			.single();
	}

	public void saveWorkoutSet(
		Integer workoutExerciseId,
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		Integer rpe,
		WorkoutSetType setType,
		boolean completed
	) {
		jdbcClient.sql("""
				INSERT INTO workout_sets (
				    workout_exercise_id,
				    set_order,
				    weight,
				    reps,
				    rpe,
				    set_type,
				    completed
				)
				VALUES (
				    :workoutExerciseId,
				    :setOrder,
				    :weight,
				    :reps,
				    :rpe,
				    :setType,
				    :completed
				)
				""")
			.param("workoutExerciseId", workoutExerciseId)
			.param("setOrder", setOrder)
			.param("weight", weight)
			.param("reps", reps)
			.param("rpe", rpe)
			.param("setType", setType.name())
			.param("completed", completed)
			.update();
	}

	private Optional<WorkoutHeader> findHeaderById(Integer workoutId) {
		return jdbcClient.sql("""
				SELECT workout_id, workout_date, workout_order, memo
				FROM workouts
				WHERE workout_id = :workoutId
				""")
			.param("workoutId", workoutId)
			.query((rs, rowNum) -> new WorkoutHeader(
				rs.getInt("workout_id"),
				rs.getObject("workout_date", LocalDate.class),
				rs.getInt("workout_order"),
				rs.getString("memo")
			))
			.optional();
	}

	private record WorkoutHeader(
		Integer workoutId,
		LocalDate workoutDate,
		Integer workoutOrder,
		String memo
	) {
	}

	private record WorkoutDetailRow(
		Integer workoutExerciseId,
		Integer exerciseId,
		String exerciseName,
		Integer exerciseOrder,
		String exerciseMemo,
		boolean exerciseCompleted,
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		Integer rpe,
		WorkoutSetType setType,
		boolean setCompleted
	) {
	}

	private static class WorkoutExerciseBuilder {
		private final WorkoutDetailRow row;
		private final List<WorkoutSetResponse> sets = new ArrayList<>();

		WorkoutExerciseBuilder(WorkoutDetailRow row) {
			this.row = row;
		}

		void addSet(WorkoutSetResponse set) {
			sets.add(set);
		}

		WorkoutExerciseResponse build() {
			return new WorkoutExerciseResponse(
				row.exerciseId(),
				row.exerciseName(),
				row.exerciseOrder(),
				row.exerciseMemo(),
				row.exerciseCompleted(),
				sets
			);
		}
	}
}
