package com.workoutlog.backend.exercise.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseCategory;
import com.workoutlog.backend.exercise.ExerciseType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExerciseRepository {
	private final JdbcClient jdbcClient;

	public ExerciseRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public List<Exercise> findActive(ExerciseCategory category) {
		if (category == null) {
			return jdbcClient.sql("""
					SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
					FROM exercises
					WHERE is_active = TRUE
					ORDER BY exercise_category, exercise_name, exercise_id
					""")
				.query(this::mapExercise)
				.list();
		}

		return jdbcClient.sql("""
				SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
				FROM exercises
				WHERE is_active = TRUE
				  AND exercise_category = :category
				ORDER BY exercise_name, exercise_id
				""")
			.param("category", category.name())
			.query(this::mapExercise)
			.list();
	}

	public Optional<Exercise> findById(Integer exerciseId) {
		return jdbcClient.sql("""
				SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
				FROM exercises
				WHERE exercise_id = :exerciseId
				""")
			.param("exerciseId", exerciseId)
			.query(this::mapExercise)
			.optional();
	}

	public Exercise saveCustom(String name, ExerciseCategory category) {
		return jdbcClient.sql("""
				INSERT INTO exercises (exercise_name, exercise_type, exercise_category)
				VALUES (:name, :type, :category)
				RETURNING exercise_id, exercise_name, exercise_type, exercise_category, is_active
				""")
			.params(Map.of(
				"name", name,
				"type", ExerciseType.CUSTOM.name(),
				"category", category.name()
			))
			.query(this::mapExercise)
			.single();
	}

	public Exercise update(Integer exerciseId, String name, ExerciseCategory category) {
		return jdbcClient.sql("""
				UPDATE exercises
				SET exercise_name = :name,
				    exercise_category = :category
				WHERE exercise_id = :exerciseId
				RETURNING exercise_id, exercise_name, exercise_type, exercise_category, is_active
				""")
			.params(Map.of(
				"exerciseId", exerciseId,
				"name", name,
				"category", category.name()
			))
			.query(this::mapExercise)
			.single();
	}

	public void deactivate(Integer exerciseId) {
		jdbcClient.sql("""
				UPDATE exercises
				SET is_active = FALSE
				WHERE exercise_id = :exerciseId
				""")
			.param("exerciseId", exerciseId)
			.update();
	}

	private Exercise mapExercise(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new Exercise(
			rs.getInt("exercise_id"),
			rs.getString("exercise_name"),
			ExerciseType.valueOf(rs.getString("exercise_type")),
			ExerciseCategory.valueOf(rs.getString("exercise_category")),
			rs.getBoolean("is_active")
		);
	}
}
