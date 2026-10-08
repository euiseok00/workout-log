package com.workoutlog.backend.workout;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.jayway.jsonpath.JsonPath;
import com.workoutlog.backend.PostgresIntegrationTest;
import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.routine.Routine;
import com.workoutlog.backend.routine.RoutineRepository;
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

class WorkoutSessionApiTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ExerciseRepository exerciseRepository;

	@Autowired
	private RoutineRepository routineRepository;

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
	void createsFreeWorkoutAndReturnsOrderedExercisesAndSets() throws Exception {
		User user = saveUser("session_free");
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer pullUpId = defaultExerciseId("풀업");
		Integer sessionId = createSession(user, LocalDate.now(), null, """
				[
				  {
				    "exerciseId": %d,
				    "exerciseOrder": 2,
				    "sets": [
				      {"setNumber": 2, "weight": 70.25, "reps": 10, "completed": false},
				      {"setNumber": 1, "weight": 60.00, "reps": 12, "completed": true}
				    ]
				  },
				  {
				    "exerciseId": %d,
				    "exerciseOrder": 1,
				    "sets": [{"setNumber": 1, "weight": 0, "reps": 8, "completed": true}]
				  }
				]
				""".formatted(benchPressId, pullUpId));

		mockMvc.perform(get("/api/workout-sessions/{sessionId}", sessionId).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.routineId").value(nullValue()))
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(pullUpId))
				.andExpect(jsonPath("$.exercises[0].name").value("풀업"))
				.andExpect(jsonPath("$.exercises[0].bodyPart").value("BACK"))
				.andExpect(jsonPath("$.exercises[1].exerciseId").value(benchPressId))
				.andExpect(jsonPath("$.exercises[1].sets[0].setNumber").value(1))
				.andExpect(jsonPath("$.exercises[1].sets[0].weight").value(60.0))
				.andExpect(jsonPath("$.exercises[1].sets[0].completed").value(true))
				.andExpect(jsonPath("$.exercises[1].sets[1].setNumber").value(2))
				.andExpect(jsonPath("$.exercises[1].sets[1].weight").value(70.25))
				.andExpect(jsonPath("$.exercises[1].sets[1].completed").value(false));
	}

	@Test
	void createsRoutineWorkoutAndListsNewestDateFirst() throws Exception {
		User user = saveUser("session_list");
		Routine routine = routineRepository.save(new Routine(user.getId(), "목록 루틴"));
		Integer exerciseId = defaultExerciseId("스쿼트");
		LocalDate olderDate = LocalDate.now().minusDays(1);

		Integer routineSessionId = createSession(user, olderDate, routine.getId(), singleExercise(exerciseId));
		Integer freeSessionId = createSession(user, LocalDate.now(), null, singleExercise(exerciseId));

		mockMvc.perform(get("/api/workout-sessions").with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(freeSessionId))
				.andExpect(jsonPath("$[0].workoutDate").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$[1].id").value(routineSessionId))
				.andExpect(jsonPath("$[1].routineId").value(routine.getId()));
	}

	@Test
	void updatesWorkoutAndReplacesExercisesAndSets() throws Exception {
		User user = saveUser("session_editor");
		Routine routine = routineRepository.save(new Routine(user.getId(), "수정 루틴"));
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer squatId = defaultExerciseId("스쿼트");
		Exercise customExercise = exerciseRepository.save(new Exercise("내 세션 운동", BodyPart.ARMS, user.getId()));
		Integer sessionId = createSession(user, LocalDate.now().minusDays(1), null,
				twoExercises(benchPressId, squatId));

		mockMvc.perform(put("/api/workout-sessions/{sessionId}", sessionId)
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(sessionBody(LocalDate.now(), routine.getId(), """
						[
						  {
						    "exerciseId": %d,
						    "exerciseOrder": 2,
						    "sets": [
						      {"setNumber": 2, "weight": 12.50, "reps": 8, "completed": true},
						      {"setNumber": 1, "weight": 10.00, "reps": 10, "completed": false}
						    ]
						  },
						  {
						    "exerciseId": %d,
						    "exerciseOrder": 1,
						    "sets": []
						  }
						]
						""".formatted(customExercise.getId(), squatId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workoutDate").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.routineId").value(routine.getId()))
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(squatId))
				.andExpect(jsonPath("$.exercises[1].exerciseId").value(customExercise.getId()))
				.andExpect(jsonPath("$.exercises[1].sets[0].setNumber").value(1))
				.andExpect(jsonPath("$.exercises[1].sets[1].setNumber").value(2))
				.andExpect(jsonPath("$.exercises[?(@.exerciseId == %d)]".formatted(benchPressId)).isEmpty());
	}

	@Test
	void hidesAnotherUsersWorkoutSession() throws Exception {
		User owner = saveUser("session_owner");
		User otherUser = saveUser("session_other");
		Integer exerciseId = defaultExerciseId("랫풀다운");
		Integer sessionId = createSession(owner, LocalDate.now(), null, singleExercise(exerciseId));

		mockMvc.perform(get("/api/workout-sessions/{sessionId}", sessionId).with(jwtFor(otherUser)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_SESSION_NOT_FOUND"));

		mockMvc.perform(put("/api/workout-sessions/{sessionId}", sessionId)
				.with(jwtFor(otherUser))
				.contentType(MediaType.APPLICATION_JSON)
				.content(sessionBody(LocalDate.now(), null, singleExercise(exerciseId))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_SESSION_NOT_FOUND"));
	}

	@Test
	void rejectsAnotherUsersRoutineAndCustomExercise() throws Exception {
		User user = saveUser("session_user");
		User otherUser = saveUser("session_blocked");
		Routine privateRoutine = routineRepository.save(new Routine(otherUser.getId(), "타인 루틴"));
		Exercise privateExercise = exerciseRepository.save(new Exercise("타인 운동", BodyPart.BACK, otherUser.getId()));
		Integer defaultExerciseId = defaultExerciseId("레그 프레스");

		assertBadRequest(user, sessionBody(LocalDate.now(), privateRoutine.getId(), singleExercise(defaultExerciseId)));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, singleExercise(privateExercise.getId())));
	}

	@Test
	void rejectsInvalidWorkoutRequests() throws Exception {
		User user = saveUser("session_valid");
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer squatId = defaultExerciseId("스쿼트");

		assertBadRequest(user, sessionBody(null, null, singleExercise(benchPressId)));
		assertBadRequest(user, sessionBody(LocalDate.now().plusDays(1), null, singleExercise(benchPressId)));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, "[]"));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, "[null]"));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, """
				[{"exerciseId": %d, "exerciseOrder": 0, "sets": []}]
				""".formatted(benchPressId)));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, """
				[
				  {"exerciseId": %d, "exerciseOrder": 1, "sets": []},
				  {"exerciseId": %d, "exerciseOrder": 1, "sets": []}
				]
				""".formatted(benchPressId, squatId)));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, invalidSetExercise(benchPressId,
				"{\"setNumber\": 0, \"weight\": 10, \"reps\": 10, \"completed\": true}")));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, """
				[{
				  "exerciseId": %d,
				  "exerciseOrder": 1,
				  "sets": [
				    {"setNumber": 1, "weight": 10, "reps": 10, "completed": true},
				    {"setNumber": 1, "weight": 20, "reps": 8, "completed": true}
				  ]
				}]
				""".formatted(benchPressId)));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, invalidSetExercise(benchPressId,
				"{\"setNumber\": 1, \"weight\": -0.01, \"reps\": 10, \"completed\": true}")));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, invalidSetExercise(benchPressId,
				"{\"setNumber\": 1, \"weight\": 1.234, \"reps\": 10, \"completed\": true}")));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, invalidSetExercise(benchPressId,
				"{\"setNumber\": 1, \"weight\": 0, \"reps\": -1, \"completed\": true}")));
		assertBadRequest(user, sessionBody(LocalDate.now(), null, singleExercise(999999)));
		assertBadRequest(user, sessionBody(LocalDate.now(), 999999, singleExercise(benchPressId)));
	}

	private Integer createSession(User user, LocalDate workoutDate, Integer routineId, String exercises)
			throws Exception {
		String response = mockMvc.perform(post("/api/workout-sessions")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(sessionBody(workoutDate, routineId, exercises)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private void assertBadRequest(User user, String body) throws Exception {
		mockMvc.perform(post("/api/workout-sessions")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	private String sessionBody(LocalDate workoutDate, Integer routineId, String exercises) {
		return """
				{
				  "workoutDate": %s,
				  "routineId": %s,
				  "exercises": %s
				}
				""".formatted(workoutDate == null ? "null" : "\"" + workoutDate + "\"",
					routineId == null ? "null" : routineId, exercises);
	}

	private String singleExercise(Integer exerciseId) {
		return """
				[{
				  "exerciseId": %d,
				  "exerciseOrder": 1,
				  "sets": [{"setNumber": 1, "weight": 0, "reps": 10, "completed": true}]
				}]
				""".formatted(exerciseId);
	}

	private String twoExercises(Integer firstExerciseId, Integer secondExerciseId) {
		return """
				[
				  {"exerciseId": %d, "exerciseOrder": 1, "sets": []},
				  {"exerciseId": %d, "exerciseOrder": 2, "sets": []}
				]
				""".formatted(firstExerciseId, secondExerciseId);
	}

	private String invalidSetExercise(Integer exerciseId, String set) {
		return """
				[{"exerciseId": %d, "exerciseOrder": 1, "sets": [%s]}]
				""".formatted(exerciseId, set);
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
