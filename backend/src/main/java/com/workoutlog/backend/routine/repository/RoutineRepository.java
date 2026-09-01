package com.workoutlog.backend.routine.repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.routine.RoutineDetail;
import com.workoutlog.backend.routine.RoutineExerciseDetail;
import com.workoutlog.backend.routine.RoutineSetType;
import com.workoutlog.backend.routine.RoutineSetDetail;
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

	public Optional<RoutineDetail> findDetailById(Integer routineId) {
		Optional<RoutineHeader> routineHeader = findHeaderById(routineId);
		if (routineHeader.isEmpty()) {
			return Optional.empty();
		}

		List<RoutineDetailRow> rows = jdbcClient.sql("""
				SELECT re.routine_exercise_id,
				       re.exercise_id,
				       e.exercise_name,
				       e.exercise_category,
				       re.exercise_order,
				       re.memo AS exercise_memo,
				       rs.set_order,
				       rs.weight,
				       rs.reps,
				       rs.set_type
				FROM routine_exercises re
				JOIN exercises e ON re.exercise_id = e.exercise_id
				LEFT JOIN routine_sets rs ON re.routine_exercise_id = rs.routine_exercise_id
				WHERE re.routine_id = :routineId
				ORDER BY re.exercise_order, rs.set_order
				""")
			.param("routineId", routineId)
			.query((rs, rowNum) -> new RoutineDetailRow(
				rs.getInt("routine_exercise_id"),
				rs.getInt("exercise_id"),
				rs.getString("exercise_name"),
				ExerciseCategory.valueOf(rs.getString("exercise_category")),
				rs.getInt("exercise_order"),
				rs.getString("exercise_memo"),
				(Integer)rs.getObject("set_order"),
				rs.getBigDecimal("weight"),
				(Integer)rs.getObject("reps"),
				rs.getString("set_type") == null ? null : RoutineSetType.valueOf(rs.getString("set_type"))
			))
			.list();

		Map<Integer, RoutineExerciseBuilder> exercises = new LinkedHashMap<>();
		for (RoutineDetailRow row : rows) {
			RoutineExerciseBuilder exercise = exercises.computeIfAbsent(
				row.routineExerciseId(),
				key -> new RoutineExerciseBuilder(row)
			);

			if (row.setOrder() != null) {
				exercise.addSet(new RoutineSetDetail(
					row.setOrder(),
					row.weight(),
					row.reps(),
					row.setType()
				));
			}
		}

		RoutineHeader header = routineHeader.get();
		return Optional.of(new RoutineDetail(
			header.routineId(),
			header.routineName(),
			header.routineMemo(),
			exercises.values()
				.stream()
				.map(RoutineExerciseBuilder::build)
				.toList()
		));
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

	public boolean existsById(Integer routineId) {
		return jdbcClient.sql("""
				SELECT EXISTS (
				    SELECT 1
				    FROM routines
				    WHERE routine_id = :routineId
				)
				""")
			.param("routineId", routineId)
			.query(Boolean.class)
			.single();
	}

	public void updateRoutine(Integer routineId, String routineName, String routineMemo) {
		jdbcClient.sql("""
				UPDATE routines
				SET routine_name = :routineName,
				    routine_memo = :routineMemo
				WHERE routine_id = :routineId
				""")
			.param("routineId", routineId)
			.param("routineName", routineName)
			.param("routineMemo", routineMemo)
			.update();
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

	public void deleteRoutineExercisesByRoutineId(Integer routineId) {
		jdbcClient.sql("""
				DELETE FROM routine_exercises
				WHERE routine_id = :routineId
				""")
			.param("routineId", routineId)
			.update();
	}

	public int deleteById(Integer routineId) {
		return jdbcClient.sql("""
				DELETE FROM routines
				WHERE routine_id = :routineId
				""")
			.param("routineId", routineId)
			.update();
	}

	private Optional<RoutineHeader> findHeaderById(Integer routineId) {
		return jdbcClient.sql("""
				SELECT routine_id, routine_name, routine_memo
				FROM routines
				WHERE routine_id = :routineId
				""")
			.param("routineId", routineId)
			.query((rs, rowNum) -> new RoutineHeader(
				rs.getInt("routine_id"),
				rs.getString("routine_name"),
				rs.getString("routine_memo")
			))
			.optional();
	}

	private List<String> toStringList(Array array) throws SQLException {
		return Arrays.asList((String[])array.getArray());
	}

	private record RoutineHeader(
		Integer routineId,
		String routineName,
		String routineMemo
	) {
	}

	private record RoutineDetailRow(
		Integer routineExerciseId,
		Integer exerciseId,
		String exerciseName,
		ExerciseCategory exerciseCategory,
		Integer exerciseOrder,
		String exerciseMemo,
		Integer setOrder,
		BigDecimal weight,
		Integer reps,
		RoutineSetType setType
	) {
	}

	private static class RoutineExerciseBuilder {
		private final RoutineDetailRow row;
		private final List<RoutineSetDetail> sets = new ArrayList<>();

		RoutineExerciseBuilder(RoutineDetailRow row) {
			this.row = row;
		}

		void addSet(RoutineSetDetail set) {
			sets.add(set);
		}

		RoutineExerciseDetail build() {
			return new RoutineExerciseDetail(
				row.exerciseId(),
				row.exerciseName(),
				row.exerciseCategory(),
				row.exerciseOrder(),
				row.exerciseMemo(),
				sets
			);
		}
	}
}
