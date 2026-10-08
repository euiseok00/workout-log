package com.workoutlog.backend.workout;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "exercise_set", uniqueConstraints =
		@UniqueConstraint(name = "uq_exercise_set_number", columnNames = { "exercise_in_session_id", "set_number" }))
public class ExerciseSet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exercise_in_session_id", nullable = false)
	private ExerciseInSession exerciseInSession;

	@Column(name = "set_number", nullable = false)
	private Integer setNumber;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal weight;

	@Column(nullable = false)
	private Integer reps;

	@Column(nullable = false)
	private boolean completed;

	protected ExerciseSet() {
	}

	public ExerciseSet(ExerciseInSession exerciseInSession, Integer setNumber, BigDecimal weight, Integer reps,
			boolean completed) {
		this.exerciseInSession = exerciseInSession;
		this.setNumber = setNumber;
		this.weight = weight;
		this.reps = reps;
		this.completed = completed;
	}

	public Integer getId() {
		return id;
	}

	public Integer getSetNumber() {
		return setNumber;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public Integer getReps() {
		return reps;
	}

	public boolean isCompleted() {
		return completed;
	}
}
