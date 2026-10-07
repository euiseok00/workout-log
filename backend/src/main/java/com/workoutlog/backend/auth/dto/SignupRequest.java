package com.workoutlog.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank
		@Pattern(regexp = "^[a-z0-9_]{4,20}$", message = "아이디는 4~20자의 영문 소문자, 숫자, 밑줄만 사용할 수 있습니다.")
		String loginId,
		@NotBlank
		@Size(min = 8, max = 72)
		String password) {
}
