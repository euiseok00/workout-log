package com.workoutlog.backend.exercise;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {

	List<Exercise> findByUserIdIsNullOrUserIdOrderById(Integer userId);
}
