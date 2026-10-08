package com.workoutlog.backend.routine;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Integer> {

	@EntityGraph(attributePaths = { "exercises", "exercises.exercise" })
	List<Routine> findAllByUserIdOrderById(Integer userId);

	@EntityGraph(attributePaths = { "exercises", "exercises.exercise" })
	Optional<Routine> findByIdAndUserId(Integer id, Integer userId);
}
