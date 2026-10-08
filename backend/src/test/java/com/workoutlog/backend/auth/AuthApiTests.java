package com.workoutlog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.workoutlog.backend.PostgresIntegrationTest;
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

class AuthApiTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
	}

	@Test
	void signupCreatesUserAndReturnsAccessToken() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody("workout_user")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.user.loginId").value("workout_user"));

		User user = userRepository.findByLoginId("workout_user").orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", user.getPasswordHash())).isTrue();
	}

	@Test
	void duplicateSignupReturnsConflict() throws Exception {
		signup("duplicate_user");

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody("duplicate_user")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
	}

	@Test
	void loginReturnsTokenAndRejectsWrongPassword() throws Exception {
		signup("login_user");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody("login_user", "password123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.user.loginId").value("login_user"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody("login_user", "wrongpass")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void availabilityAlwaysReturnsOkWithBooleanResult() throws Exception {
		mockMvc.perform(get("/api/auth/login-id/availability").param("loginId", "available_user"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(true));

		signup("available_user");

		mockMvc.perform(get("/api/auth/login-id/availability").param("loginId", "available_user"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(false));
	}

	@Test
	void blankLoginIdIsRejectedByValidation() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[?(@.field == 'loginId')]").exists());
	}

	private void signup(String loginId) throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody(loginId)))
				.andExpect(status().isCreated());
	}

	private String signupBody(String loginId) {
		return """
				{
				  "loginId": "%s",
				  "password": "password123"
				}
				""".formatted(loginId);
	}

	private String loginBody(String loginId, String password) {
		return """
				{
				  "loginId": "%s",
				  "password": "%s"
				}
				""".formatted(loginId, password);
	}
}
