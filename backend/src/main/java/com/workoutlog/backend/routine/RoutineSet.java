package com.workoutlog.backend.routine;

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
@Table(name = "routine_set", uniqueConstraints =
		@UniqueConstraint(name = "uq_routine_set_number", columnNames = { "exercise_in_routine_id", "set_number" }))
public class RoutineSet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exercise_in_routine_id", nullable = false)
	private ExerciseInRoutine exerciseInRoutine;

	@Column(name = "set_number", nullable = false)
	private Integer setNumber;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal weight;

	@Column(nullable = false)
	private Integer reps;

	protected RoutineSet() {
	}

	public RoutineSet(ExerciseInRoutine exerciseInRoutine, Integer setNumber, BigDecimal weight, Integer reps) {
		this.exerciseInRoutine = exerciseInRoutine;
		this.setNumber = setNumber;
		this.weight = weight;
		this.reps = reps;
	}

	public Integer getSetNumber() { return setNumber; }
	public BigDecimal getWeight() { return weight; }
	public Integer getReps() { return reps; }
}
