package com.workoutlog.backend.auth.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void meReturnsJwtSubject() throws Exception {
		mockMvc.perform(get("/api/auth/me")
				.with(jwt().jwt(token -> token.subject("user-123"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value("user-123"));
	}

	@Test
	void apiRequiresAuthorization() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized());
	}
}
