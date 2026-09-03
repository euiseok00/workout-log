package com.workoutlog.backend.statistics.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.statistics.dto.CategoryStatisticsPointResponse;
import com.workoutlog.backend.workout.WorkoutSetType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsRepository {
	private final JdbcClient jdbcClient;

	public StatisticsRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<ExerciseStatisticsSetRow> findExerciseSetRows(
		UUID userId,
		Integer exerciseId,
		LocalDate fromDate,
		LocalDate toDate
	) {
		String fromFilter = fromDate == null ? "" : "  AND w.workout_date >= :fromDate\n";
		JdbcClient.StatementSpec query = jdbcClient.sql("""
				SELECT w.workout_date,
				       w.workout_id,
				       w.workout_order,
				       ws.set_order,
				       ws.weight,
				       ws.reps,
				       ws.set_type,
				       ws.completed
				FROM workouts w
				JOIN workout_exercises we ON w.workout_id = we.workout_id
				JOIN workout_sets ws ON we.workout_exercise_id = ws.workout_exercise_id
				WHERE w.user_id = :userId
				  AND we.exercise_id = :exerciseId
				  AND w.workout_date <= :toDate
				""" + fromFilter + """
				ORDER BY w.workout_date, w.workout_order, we.exercise_order, ws.set_order
				""")
			.param("userId", userId)
			.param("exerciseId", exerciseId)
			.param("toDate", toDate);

		if (fromDate != null) {
			query = query.param("fromDate", fromDate);
		}

		return query
			.query((rs, rowNum) -> new ExerciseStatisticsSetRow(
				rs.getObject("workout_date", LocalDate.class),
				rs.getInt("workout_id"),
				rs.getInt("workout_order"),
				rs.getInt("set_order"),
				rs.getBigDecimal("weight"),
				rs.getInt("reps"),
				WorkoutSetType.valueOf(rs.getString("set_type")),
				rs.getBoolean("completed")
			))
			.list();
	}

	public List<CategoryStatisticsPointResponse> findCategoryVolumes(
		UUID userId,
		ExerciseCategory category,
		LocalDate fromDate,
		LocalDate toDate
	) {
		String fromFilter = fromDate == null ? "" : "  AND w.workout_date >= :fromDate\n";
		JdbcClient.StatementSpec query = jdbcClient.sql("""
				SELECT w.workout_date,
				       SUM(ws.weight * ws.reps) AS volume
				FROM workouts w
				JOIN workout_exercises we ON w.workout_id = we.workout_id
				JOIN workout_sets ws ON we.workout_exercise_id = ws.workout_exercise_id
				WHERE w.user_id = :userId
				  AND we.exercise_category = :category
				  AND w.workout_date <= :toDate
				""" + fromFilter + """
				  AND ws.completed = TRUE
				  AND ws.set_type IN ('WORKING', 'TOP', 'FAILURE', 'BACKOFF', 'DROP')
				  AND ws.weight IS NOT NULL
				  AND ws.weight > 0
				GROUP BY w.workout_date
				ORDER BY w.workout_date
				""")
			.param("userId", userId)
			.param("category", category.name())
			.param("toDate", toDate);

		if (fromDate != null) {
			query = query.param("fromDate", fromDate);
		}

		return query
			.query((rs, rowNum) -> new CategoryStatisticsPointResponse(
				rs.getObject("workout_date", LocalDate.class),
				rs.getBigDecimal("volume")
			))
			.list();
	}

	public record ExerciseStatisticsSetRow(
		LocalDate date,
		Integer workoutId,
		Integer workoutOrder,
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		WorkoutSetType setType,
		boolean completed
	) {
	}
}
