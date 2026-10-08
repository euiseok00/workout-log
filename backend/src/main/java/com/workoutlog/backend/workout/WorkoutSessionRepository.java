package com.workoutlog.backend.workout;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Integer> {

	@EntityGraph(attributePaths = { "exercises", "exercises.exercise" })
	@Query("""
			select distinct session from WorkoutSession session
			where session.userId = :userId
			  and session.workoutDate >= coalesce(:fromDate, session.workoutDate)
			  and session.workoutDate <= coalesce(:toDate, session.workoutDate)
			order by session.workoutDate desc, session.id desc
			""")
	List<WorkoutSession> findAllInDateRange(@Param("userId") Integer userId,
			@Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

	@EntityGraph(attributePaths = { "exercises", "exercises.exercise" })
	Optional<WorkoutSession> findByIdAndUserId(Integer id, Integer userId);
}
