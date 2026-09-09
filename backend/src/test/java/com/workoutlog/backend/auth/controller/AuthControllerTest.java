package com.workoutlog.backend.auth.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

	@Test
	void apiOptionsPreflightDoesNotRequireAuthorization() throws Exception {
		mockMvc.perform(options("/api/exercises")
				.header("Origin", "https://workout-log-euiseok00.vercel.app")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "Authorization, Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "https://workout-log-euiseok00.vercel.app"));
	}
}
