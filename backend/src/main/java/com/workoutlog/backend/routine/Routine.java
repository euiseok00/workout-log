package com.workoutlog.backend.routine;

import java.time.Instant;
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
@Table(name = "routine")
public class Routine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("exerciseOrder ASC")
	private List<ExerciseInRoutine> exercises = new ArrayList<>();

	protected Routine() {
	}

	public Routine(Integer userId, String name) {
		this.userId = userId;
		this.name = name;
		this.createdAt = Instant.now();
		this.updatedAt = createdAt;
	}

	public void update(String name) {
		this.name = name;
		this.updatedAt = Instant.now();
	}

	public void addExercise(ExerciseInRoutine exercise) {
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

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<ExerciseInRoutine> getExercises() {
		return exercises;
	}
}
