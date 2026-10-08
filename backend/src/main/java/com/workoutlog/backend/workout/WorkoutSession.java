package com.workoutlog.backend.workout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_session")
public class WorkoutSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "routine_id")
	private Integer routineId;

	@Column(name = "workout_date", nullable = false)
	private LocalDate workoutDate;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("exerciseOrder ASC")
	private List<ExerciseInSession> exercises = new ArrayList<>();

	protected WorkoutSession() {
	}

	public WorkoutSession(Integer userId, Integer routineId, LocalDate workoutDate) {
		this.userId = userId;
		this.routineId = routineId;
		this.workoutDate = workoutDate;
	}

	public void update(Integer routineId, LocalDate workoutDate) {
		this.routineId = routineId;
		this.workoutDate = workoutDate;
	}

	public void addExercise(ExerciseInSession exercise) {
		exercises.add(exercise);
	}

	public void clearExercises() {
		exercises.clear();
	}

	public Integer getId() {
		return id;
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getRoutineId() {
		return routineId;
	}

	public LocalDate getWorkoutDate() {
		return workoutDate;
	}

	public List<ExerciseInSession> getExercises() {
		return exercises;
	}
}
