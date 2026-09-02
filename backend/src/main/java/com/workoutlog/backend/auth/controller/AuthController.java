package com.workoutlog.backend.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@GetMapping("/me")
	MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new MeResponse(jwt.getSubject());
	}

	record MeResponse(String userId) {
	}
}
