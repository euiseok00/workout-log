package com.workoutlog.backend.routine;

import com.workoutlog.backend.exercise.Exercise;

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
@Table(name = "exercise_in_routine", uniqueConstraints = {
		@UniqueConstraint(name = "uq_exercise_in_routine_order", columnNames = { "routine_id", "exercise_order" }),
		@UniqueConstraint(name = "uq_exercise_in_routine_exercise", columnNames = { "routine_id", "exercise_id" })
})
public class ExerciseInRoutine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "routine_id", nullable = false)
	private Routine routine;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exercise_id", nullable = false)
	private Exercise exercise;

	@Column(name = "exercise_order", nullable = false)
	private Integer exerciseOrder;

	@Column(name = "target_sets")
	private Integer targetSets;

	@Column(name = "target_reps")
	private Integer targetReps;

	protected ExerciseInRoutine() {
	}

	public ExerciseInRoutine(Routine routine, Exercise exercise, Integer exerciseOrder, Integer targetSets,
			Integer targetReps) {
		this.routine = routine;
		this.exercise = exercise;
		this.exerciseOrder = exerciseOrder;
		this.targetSets = targetSets;
		this.targetReps = targetReps;
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

	public Integer getTargetSets() {
		return targetSets;
	}

	public Integer getTargetReps() {
		return targetReps;
	}
}
