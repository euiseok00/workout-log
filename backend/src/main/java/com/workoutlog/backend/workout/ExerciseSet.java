package com.workoutlog.backend.workout;

import java.math.BigDecimal;

import com.workoutlog.backend.exercise.SetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "set_type", nullable = false, length = 20)
	private SetType setType = SetType.WORKING;

	protected ExerciseSet() {
	}

	public ExerciseSet(ExerciseInSession exerciseInSession, Integer setNumber, BigDecimal weight, Integer reps,
			boolean completed, SetType setType) {
		this.exerciseInSession = exerciseInSession;
		this.setNumber = setNumber;
		this.weight = weight;
		this.reps = reps;
		this.completed = completed;
		this.setType = setType == null ? SetType.WORKING : setType;
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

	public SetType getSetType() {
		return setType;
	}

	public boolean isCompleted() {
		return completed;
	}
}
