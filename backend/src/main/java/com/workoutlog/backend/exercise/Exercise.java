package com.workoutlog.backend.exercise;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercise")
public class Exercise {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_part", nullable = false, length = 20)
	private BodyPart bodyPart;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Exercise() {
	}

	public Exercise(String name, BodyPart bodyPart, Integer userId) {
		this.name = name;
		this.bodyPart = bodyPart;
		this.userId = userId;
		this.createdAt = Instant.now();
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BodyPart getBodyPart() {
		return bodyPart;
	}

	public Integer getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
