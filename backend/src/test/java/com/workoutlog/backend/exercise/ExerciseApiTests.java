package com.workoutlog.backend.exercise;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
	private JdbcTemplate jdbcTemplate;

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

	@Test
	void returnsExerciseRecordsNewestFirstWithOrderedSetsForCurrentUserOnly() throws Exception {
		User currentUser = saveUser("record_user");
		User otherUser = saveUser("record_other");
		Integer exerciseId = defaultExerciseId("벤치프레스");
		LocalDate today = LocalDate.now();
		createSession(currentUser, today.minusDays(2), exerciseId, """
				{"setNumber":2,"weight":70.00,"reps":8,"completed":false},
				{"setNumber":1,"weight":60.00,"reps":12,"completed":true}
				""");
		createSession(currentUser, today, exerciseId,
				"{\"setNumber\":1,\"weight\":75.50,\"reps\":6,\"completed\":true}");
		createSession(otherUser, today, exerciseId,
				"{\"setNumber\":1,\"weight\":200.00,\"reps\":20,\"completed\":true}");

		mockMvc.perform(get("/api/exercises/{exerciseId}/records", exerciseId).with(jwtFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].workoutDate").value(today.toString()))
				.andExpect(jsonPath("$[0].exerciseId").value(exerciseId))
				.andExpect(jsonPath("$[0].exerciseName").value("벤치프레스"))
				.andExpect(jsonPath("$[0].sets[0].weight").value(75.5))
				.andExpect(jsonPath("$[1].workoutDate").value(today.minusDays(2).toString()))
				.andExpect(jsonPath("$[1].sets[0].setNumber").value(1))
				.andExpect(jsonPath("$[1].sets[0].weight").value(60.0))
				.andExpect(jsonPath("$[1].sets[0].reps").value(12))
				.andExpect(jsonPath("$[1].sets[0].completed").value(true))
				.andExpect(jsonPath("$[1].sets[1].setNumber").value(2))
				.andExpect(jsonPath("$[1].sets[1].completed").value(false));
	}

	@Test
	void returnsEmptyForOwnExerciseWithoutRecordsAndRejectsAnotherUsersExercise() throws Exception {
		User currentUser = saveUser("record_empty");
		User otherUser = saveUser("record_private");
		Exercise ownExercise = exerciseRepository.save(new Exercise("내 운동", BodyPart.ARMS, currentUser.getId()));
		Exercise privateExercise = exerciseRepository.save(new Exercise("타인 운동", BodyPart.BACK, otherUser.getId()));

		mockMvc.perform(get("/api/exercises/{exerciseId}/records", ownExercise.getId()).with(jwtFor(currentUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/exercises/{exerciseId}/records", privateExercise.getId()).with(jwtFor(currentUser)))
				.andExpect(status().isNotFound());
	}

	private void createSession(User user, LocalDate workoutDate, Integer exerciseId, String sets) throws Exception {
		mockMvc.perform(post("/api/workout-sessions")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "workoutDate":"%s",
						  "routineId":null,
						  "exercises":[{
						    "exerciseId":%d,
						    "exerciseOrder":1,
						    "sets":[%s]
						  }]
						}
						""".formatted(workoutDate, exerciseId, sets)))
				.andExpect(status().isCreated());
	}

	private Integer defaultExerciseId(String name) {
		return exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() == null && exercise.getName().equals(name))
				.findFirst()
				.orElseThrow()
				.getId();
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
		jdbcTemplate.update("DELETE FROM exercise_set");
		jdbcTemplate.update("DELETE FROM exercise_in_session");
		jdbcTemplate.update("DELETE FROM workout_session");
		jdbcTemplate.update("DELETE FROM exercise_in_routine");
		jdbcTemplate.update("DELETE FROM routine");
		exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() != null)
				.toList());
		userRepository.deleteAll();
	}
}
