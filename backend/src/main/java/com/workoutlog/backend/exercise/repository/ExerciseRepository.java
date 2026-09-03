package com.workoutlog.backend.exercise.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

	public List<Exercise> findActive(UUID userId, ExerciseCategory category) {
		if (category == null) {
			return jdbcClient.sql("""
					SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
					FROM exercises
					WHERE is_active = TRUE
					  AND (
					      exercise_type = 'SYSTEM'
					      OR (exercise_type = 'CUSTOM' AND user_id = :userId)
					  )
					ORDER BY exercise_category, exercise_name, exercise_id
					""")
				.param("userId", userId)
				.query(this::mapExercise)
				.list();
		}

		return jdbcClient.sql("""
				SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
				FROM exercises
				WHERE is_active = TRUE
				  AND (
				      exercise_type = 'SYSTEM'
				      OR (exercise_type = 'CUSTOM' AND user_id = :userId)
				  )
				  AND exercise_category = :category
				ORDER BY exercise_name, exercise_id
				""")
			.param("userId", userId)
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

	public Optional<Exercise> findAvailableById(UUID userId, Integer exerciseId) {
		return jdbcClient.sql("""
				SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
				FROM exercises
				WHERE exercise_id = :exerciseId
				  AND (
				      exercise_type = 'SYSTEM'
				      OR (exercise_type = 'CUSTOM' AND user_id = :userId)
				  )
				""")
			.param("exerciseId", exerciseId)
			.param("userId", userId)
			.query(this::mapExercise)
			.optional();
	}

	public Optional<Exercise> findCustomById(UUID userId, Integer exerciseId) {
		return jdbcClient.sql("""
				SELECT exercise_id, exercise_name, exercise_type, exercise_category, is_active
				FROM exercises
				WHERE exercise_id = :exerciseId
				  AND exercise_type = :type
				  AND user_id = :userId
				""")
			.param("exerciseId", exerciseId)
			.param("type", ExerciseType.CUSTOM.name())
			.param("userId", userId)
			.query(this::mapExercise)
			.optional();
	}

	public Exercise saveCustom(UUID userId, String name, ExerciseCategory category) {
		return jdbcClient.sql("""
				INSERT INTO exercises (user_id, exercise_name, exercise_type, exercise_category)
				VALUES (:userId, :name, :type, :category)
				RETURNING exercise_id, exercise_name, exercise_type, exercise_category, is_active
				""")
			.params(Map.of(
				"userId", userId,
				"name", name,
				"type", ExerciseType.CUSTOM.name(),
				"category", category.name()
			))
			.query(this::mapExercise)
			.single();
	}

	public Exercise update(UUID userId, Integer exerciseId, String name, ExerciseCategory category) {
		return jdbcClient.sql("""
				UPDATE exercises
				SET exercise_name = :name,
				    exercise_category = :category
				WHERE exercise_id = :exerciseId
				  AND exercise_type = :type
				  AND user_id = :userId
				RETURNING exercise_id, exercise_name, exercise_type, exercise_category, is_active
				""")
			.params(Map.of(
				"userId", userId,
				"exerciseId", exerciseId,
				"type", ExerciseType.CUSTOM.name(),
				"name", name,
				"category", category.name()
			))
			.query(this::mapExercise)
			.single();
	}

	public void deactivate(UUID userId, Integer exerciseId) {
		jdbcClient.sql("""
				UPDATE exercises
				SET is_active = FALSE
				WHERE exercise_id = :exerciseId
				  AND exercise_type = :type
				  AND user_id = :userId
				""")
			.param("userId", userId)
			.param("exerciseId", exerciseId)
			.param("type", ExerciseType.CUSTOM.name())
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
