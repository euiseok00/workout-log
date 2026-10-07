package com.workoutlog.backend.auth.dto;

import java.time.Instant;

import com.workoutlog.backend.user.User;

public record AuthUserResponse(Long id, String loginId, Instant createdAt) {

	public static AuthUserResponse from(User user) {
		return new AuthUserResponse(user.getId(), user.getLoginId(), user.getCreatedAt());
	}
}
