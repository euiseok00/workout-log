package com.workoutlog.backend.workout;

import java.util.ArrayList;
import java.util.List;

import com.workoutlog.backend.exercise.Exercise;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "exercise_in_session", uniqueConstraints =
		@UniqueConstraint(name = "uq_exercise_in_session_order", columnNames = { "session_id", "exercise_order" }))
public class ExerciseInSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private WorkoutSession session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exercise_id", nullable = false)
	private Exercise exercise;

	@Column(name = "exercise_order", nullable = false)
	private Integer exerciseOrder;

	@OneToMany(mappedBy = "exerciseInSession", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("setNumber ASC")
	private List<ExerciseSet> sets = new ArrayList<>();

	protected ExerciseInSession() {
	}

	public ExerciseInSession(WorkoutSession session, Exercise exercise, Integer exerciseOrder) {
		this.session = session;
		this.exercise = exercise;
		this.exerciseOrder = exerciseOrder;
	}

	public void addSet(ExerciseSet set) {
		sets.add(set);
	}

	public Integer getId() {
		return id;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public Integer getExerciseOrder() {
		return exerciseOrder;
	}

	public List<ExerciseSet> getSets() {
		return sets;
	}
}
