package com.workoutlog.backend.routine.repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.workoutlog.backend.routine.RoutineSetType;
import com.workoutlog.backend.routine.RoutineSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RoutineRepository {
	private final JdbcClient jdbcClient;

	public RoutineRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<RoutineSummary> findAll() {
		return jdbcClient.sql("""
				SELECT r.routine_id,
				       r.routine_name,
				       r.routine_memo,
				       COALESCE(
				           ARRAY_AGG(e.exercise_name ORDER BY re.exercise_order)
				               FILTER (WHERE e.exercise_name IS NOT NULL),
				           ARRAY[]::text[]
				       ) AS exercise_names
				FROM routines r
				LEFT JOIN routine_exercises re ON r.routine_id = re.routine_id
				LEFT JOIN exercises e ON re.exercise_id = e.exercise_id
				GROUP BY r.routine_id, r.routine_name, r.routine_memo
				ORDER BY r.routine_id DESC
				""")
			.query((rs, rowNum) -> new RoutineSummary(
				rs.getInt("routine_id"),
				rs.getString("routine_name"),
				rs.getString("routine_memo"),
				toStringList(rs.getArray("exercise_names"))
			))
			.list();
	}

	public Integer saveRoutine(String routineName, String routineMemo) {
		return jdbcClient.sql("""
				INSERT INTO routines (routine_name, routine_memo)
				VALUES (:routineName, :routineMemo)
				RETURNING routine_id
				""")
			.param("routineName", routineName)
			.param("routineMemo", routineMemo)
			.query(Integer.class)
			.single();
	}

	public Integer saveRoutineExercise(
		Integer routineId,
		Integer exerciseId,
		Integer exerciseOrder,
		String memo
	) {
		return jdbcClient.sql("""
				INSERT INTO routine_exercises (routine_id, exercise_id, exercise_order, memo)
				VALUES (:routineId, :exerciseId, :exerciseOrder, :memo)
				RETURNING routine_exercise_id
				""")
			.param("routineId", routineId)
			.param("exerciseId", exerciseId)
			.param("exerciseOrder", exerciseOrder)
			.param("memo", memo)
			.query(Integer.class)
			.single();
	}

	public void saveRoutineSet(
		Integer routineExerciseId,
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		RoutineSetType setType
	) {
		jdbcClient.sql("""
				INSERT INTO routine_sets (routine_exercise_id, set_order, weight, reps, set_type)
				VALUES (:routineExerciseId, :setOrder, :weight, :reps, :setType)
				""")
			.param("routineExerciseId", routineExerciseId)
			.param("setOrder", setOrder)
			.param("weight", weight)
			.param("reps", reps)
			.param("setType", setType.name())
			.update();
	}

	private List<String> toStringList(Array array) throws SQLException {
		return Arrays.asList((String[])array.getArray());
	}
}
