package com.workoutlog.backend.exercise;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.workoutlog.backend.PostgresIntegrationTest;
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

class ExerciseApiTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ExerciseRepository exerciseRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void clearBeforeTest() {
		clearDatabase();
	}

	@AfterEach
	void clearAfterTest() {
		clearDatabase();
	}

	@Test
	void returnsSystemAndCurrentUsersExercisesOnly() throws Exception {
		User currentUser = saveUser("current_user");
		User otherUser = saveUser("other_user");
		exerciseRepository.save(new Exercise("해머 컬", BodyPart.ARMS, currentUser.getId()));
		exerciseRepository.save(new Exercise("다른 사용자 운동", BodyPart.BACK, otherUser.getId()));

		mockMvc.perform(get("/api/exercises").with(jwtFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(26))
				.andExpect(jsonPath("$[?(@.name == '벤치프레스' && @.custom == false)]").exists())
				.andExpect(jsonPath("$[?(@.name == '해머 컬' && @.custom == true)]").exists())
				.andExpect(jsonPath("$[?(@.name == '다른 사용자 운동')]").isEmpty());
	}

	@Test
	void returns25DefaultExercisesSeededByFlyway() throws Exception {
		User currentUser = saveUser("seed_user");

		mockMvc.perform(get("/api/exercises").with(jwtFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(25))
				.andExpect(jsonPath("$[?(@.bodyPart == 'CHEST')]").value(hasSize(5)))
				.andExpect(jsonPath("$[?(@.bodyPart == 'BACK')]").value(hasSize(5)))
				.andExpect(jsonPath("$[?(@.bodyPart == 'SHOULDERS')]").value(hasSize(5)))
				.andExpect(jsonPath("$[?(@.bodyPart == 'ARMS')]").value(hasSize(5)))
				.andExpect(jsonPath("$[?(@.bodyPart == 'LEGS')]").value(hasSize(5)));
	}

	@Test
	void createsExerciseForAuthenticatedUserAndReturnsIt() throws Exception {
		User currentUser = saveUser("creator_user");

		mockMvc.perform(post("/api/exercises")
				.with(jwtFor(currentUser))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "해머 컬",
						  "bodyPart": "ARMS"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("해머 컬"))
				.andExpect(jsonPath("$.bodyPart").value("ARMS"))
				.andExpect(jsonPath("$.custom").value(true))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		mockMvc.perform(get("/api/exercises").with(jwtFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == '해머 컬' && @.custom == true)]").exists());
	}

	@Test
	void rejectsInvalidExerciseRequests() throws Exception {
		User currentUser = saveUser("validation_user");

		assertBadRequest(currentUser, """
				{
				  "name": "   ",
				  "bodyPart": "ARMS"
				}
				""");
		assertBadRequest(currentUser, """
				{
				  "name": "%s",
				  "bodyPart": "ARMS"
				}
				""".formatted("a".repeat(101)));
		assertBadRequest(currentUser, """
				{
				  "name": "운동"
				}
				""");
		assertBadRequest(currentUser, """
				{
				  "name": "운동",
				  "bodyPart": "CORE"
				}
				""");
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/exercises"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/exercises")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "해머 컬",
						  "bodyPart": "ARMS"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	private User saveUser(String loginId) {
		return userRepository.save(new User(loginId, "password-hash"));
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(User user) {
		return jwt().jwt(jwt -> jwt.subject(user.getId().toString()));
	}

	private void assertBadRequest(User user, String body) throws Exception {
		mockMvc.perform(post("/api/exercises")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest());
	}

	private void clearDatabase() {
		exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() != null)
				.toList());
		userRepository.deleteAll();
	}
}
