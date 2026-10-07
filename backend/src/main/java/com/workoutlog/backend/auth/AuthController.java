package com.workoutlog.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workoutlog.backend.auth.dto.AuthResponse;
import com.workoutlog.backend.auth.dto.LoginIdAvailabilityResponse;
import com.workoutlog.backend.auth.dto.LoginRequest;
import com.workoutlog.backend.auth.dto.SignupRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/login-id/availability")
	public LoginIdAvailabilityResponse checkLoginIdAvailability(
			@RequestParam
			@NotBlank
			@Pattern(regexp = "^[a-z0-9_]{4,20}$", message = "아이디는 4~20자의 영문 소문자, 숫자, 밑줄만 사용할 수 있습니다.")
			String loginId) {
		return authService.checkLoginIdAvailability(loginId);
	}
}
