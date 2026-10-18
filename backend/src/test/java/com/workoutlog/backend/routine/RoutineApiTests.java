package com.workoutlog.backend.routine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

class RoutineApiTests extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void createsListsAndReturnsRoutineDetailInExerciseOrder() throws Exception {
		User user = saveUser("routine_owner");
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer cableFlyId = defaultExerciseId("케이블 플라이");

		Integer routineId = createRoutine(user, "가슴 루틴", """
				[
				  {"exerciseId": %d, "exerciseOrder": 2, "targetSets": 4, "targetReps": 8},
				  {"exerciseId": %d, "exerciseOrder": 1, "targetSets": 3, "targetReps": 10}
				]
				""".formatted(benchPressId, cableFlyId));

		mockMvc.perform(get("/api/routines").with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(routineId))
				.andExpect(jsonPath("$[0].name").value("가슴 루틴"));

		mockMvc.perform(get("/api/routines/{routineId}", routineId).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises.length()").value(2))
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(cableFlyId))
				.andExpect(jsonPath("$.exercises[0].name").value("케이블 플라이"))
				.andExpect(jsonPath("$.exercises[0].bodyPart").value("CHEST"))
				.andExpect(jsonPath("$.exercises[0].exerciseOrder").value(1))
				.andExpect(jsonPath("$.exercises[1].exerciseId").value(benchPressId))
				.andExpect(jsonPath("$.exercises[1].exerciseOrder").value(2));
	}

	@Test
	void updatesNameAndReplacesExerciseConfiguration() throws Exception {
		User user = saveUser("routine_editor");
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer squatId = defaultExerciseId("스쿼트");
		Exercise customExercise = exerciseRepository.save(new Exercise("내 운동", BodyPart.ARMS, user.getId()));
		Integer routineId = createRoutine(user, "기존 루틴", """
				[
				  {"exerciseId": %d, "exerciseOrder": 1, "targetSets": 3, "targetReps": 8},
				  {"exerciseId": %d, "exerciseOrder": 2, "targetSets": 3, "targetReps": 10}
				]
				""".formatted(benchPressId, squatId));

		mockMvc.perform(put("/api/routines/{routineId}", routineId)
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(routineBody("수정 루틴", """
						[
						  {"exerciseId": %d, "exerciseOrder": 2, "targetSets": 4, "targetReps": 6},
						  {"exerciseId": %d, "exerciseOrder": 1, "targetSets": 5, "targetReps": 12}
						]
						""".formatted(benchPressId, customExercise.getId()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("수정 루틴"))
				.andExpect(jsonPath("$.exercises.length()").value(2))
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(customExercise.getId()))
				.andExpect(jsonPath("$.exercises[1].exerciseId").value(benchPressId))
				.andExpect(jsonPath("$.exercises[?(@.exerciseId == %d)]".formatted(squatId)).isEmpty());
	}

	@Test
	void hidesAnotherUsersRoutine() throws Exception {
		User owner = saveUser("private_owner");
		User otherUser = saveUser("private_other");
		Integer exerciseId = defaultExerciseId("랫풀다운");
		Integer routineId = createRoutine(owner, "비공개 루틴", singleExercise(exerciseId));

		mockMvc.perform(get("/api/routines/{routineId}", routineId).with(jwtFor(otherUser)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));

		mockMvc.perform(put("/api/routines/{routineId}", routineId)
				.with(jwtFor(otherUser))
				.contentType(MediaType.APPLICATION_JSON)
				.content(routineBody("탈취 시도", singleExercise(exerciseId))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));

		mockMvc.perform(delete("/api/routines/{routineId}", routineId).with(jwtFor(otherUser)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletesRoutineAndSetsWhilePreservingCompletedSession() throws Exception {
		User owner = saveUser("delete_owner");
		Integer exerciseId = defaultExerciseId("벤치프레스");
		Integer routineId = createRoutine(owner, "삭제할 루틴", """
				[{"exerciseId":%d,"exerciseOrder":1,"targetSets":2,"targetReps":8,
				"sets":[{"setNumber":1,"weight":60.50,"reps":8},
				{"setNumber":2,"weight":70.00,"reps":6}]}]
				""".formatted(exerciseId));
		mockMvc.perform(get("/api/routines/{id}", routineId).with(jwtFor(owner)))
				.andExpect(jsonPath("$.exercises[0].sets[0].weight").value(60.5))
				.andExpect(jsonPath("$.exercises[0].sets[1].reps").value(6));
		String sessionBody = """
				{"workoutDate":"%s","routineId":%d,"exercises":[{"exerciseId":%d,
				"exerciseOrder":1,"sets":[{"setNumber":1,"weight":60.50,"reps":8,"completed":true}]}]}
				""".formatted(java.time.LocalDate.now(), routineId, exerciseId);
		String sessionResponse = mockMvc.perform(post("/api/workout-sessions").with(jwtFor(owner))
				.contentType(MediaType.APPLICATION_JSON).content(sessionBody))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Integer sessionId = JsonPath.read(sessionResponse, "$.id");

		mockMvc.perform(delete("/api/routines/{id}", routineId).with(jwtFor(owner)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/routines/{id}", routineId).with(jwtFor(owner)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/routines").with(jwtFor(owner)))
				.andExpect(jsonPath("$.length()").value(0));
		org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM routine_set", Integer.class)).isZero();
		mockMvc.perform(get("/api/workout-sessions/{id}", sessionId).with(jwtFor(owner)))
				.andExpect(jsonPath("$.routineId").value(routineId))
				.andExpect(jsonPath("$.routineName").value("삭제할 루틴"))
				.andExpect(jsonPath("$.exercises[0].sets[0].weight").value(60.5));
		mockMvc.perform(put("/api/workout-sessions/{id}", sessionId).with(jwtFor(owner))
				.contentType(MediaType.APPLICATION_JSON).content(sessionBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.routineName").value("삭제할 루틴"));
	}

	@Test
	void rejectsAnotherUsersCustomExercise() throws Exception {
		User user = saveUser("routine_user");
		User otherUser = saveUser("routine_other");
		Exercise privateExercise = exerciseRepository.save(new Exercise("타인 운동", BodyPart.BACK, otherUser.getId()));

		assertBadRequest(user, routineBody("잘못된 루틴", singleExercise(privateExercise.getId())));
	}

	@Test
	void rejectsInvalidRoutineRequests() throws Exception {
		User user = saveUser("routine_validation");
		Integer benchPressId = defaultExerciseId("벤치프레스");
		Integer squatId = defaultExerciseId("스쿼트");

		assertBadRequest(user, routineBody("   ", singleExercise(benchPressId)));
		assertBadRequest(user, routineBody("빈 루틴", "[]"));
		assertBadRequest(user, routineBody("빈 운동", "[null]"));
		assertBadRequest(user, routineBody("순서 오류", """
				[{"exerciseId": %d, "exerciseOrder": 0, "targetSets": 3, "targetReps": 8}]
				""".formatted(benchPressId)));
		assertBadRequest(user, routineBody("목표 오류", """
				[{"exerciseId": %d, "exerciseOrder": 1, "targetSets": 0, "targetReps": 0}]
				""".formatted(benchPressId)));
		assertBadRequest(user, routineBody("순서 중복", """
				[
				  {"exerciseId": %d, "exerciseOrder": 1},
				  {"exerciseId": %d, "exerciseOrder": 1}
				]
				""".formatted(benchPressId, squatId)));
		assertBadRequest(user, routineBody("없는 운동", singleExercise(999999)));
		assertBadRequest(user, routineBody("잘못된 세트", """
				[{"exerciseId":%d,"exerciseOrder":1,"sets":[
				{"setNumber":1,"weight":10,"reps":8},
				{"setNumber":1,"weight":12,"reps":6}]}]
				""".formatted(benchPressId)));
	}

	@Test
	void savesFreeSessionSetsAsIndependentRoutinesAndKeepsOriginal() throws Exception {
		User owner = saveUser("session_owner");
		User other = saveUser("session_other");
		Integer exerciseId = defaultExerciseId("벤치프레스");
		String sessionResponse = mockMvc.perform(post("/api/workout-sessions").with(jwtFor(owner))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"workoutDate":"%s","exercises":[{"exerciseId":%d,"exerciseOrder":1,
						"sets":[{"setNumber":1,"weight":72.50,"reps":8,"completed":true},
						{"setNumber":2,"weight":75.00,"reps":6,"completed":true}]}]}
						""".formatted(java.time.LocalDate.now(), exerciseId)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Integer sessionId = JsonPath.read(sessionResponse, "$.id");
		String body = "{\"name\":\"기록 루틴\"}";
		mockMvc.perform(post("/api/routines/from-session/{id}", sessionId).with(jwtFor(other))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
		Integer firstId = saveSessionRoutine(owner, sessionId, body);
		Integer secondId = saveSessionRoutine(owner, sessionId, body);
		org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
		mockMvc.perform(get("/api/routines").with(jwtFor(owner)))
				.andExpect(jsonPath("$.length()").value(2));
		mockMvc.perform(get("/api/routines/{id}", firstId).with(jwtFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(exerciseId))
				.andExpect(jsonPath("$.exercises[0].sets[0].weight").value(72.5))
				.andExpect(jsonPath("$.exercises[0].sets[0].reps").value(8))
				.andExpect(jsonPath("$.exercises[0].sets[1].weight").value(75.0))
				.andExpect(jsonPath("$.exercises[0].sets[1].reps").value(6));
		mockMvc.perform(put("/api/routines/{id}", firstId).with(jwtFor(owner))
				.contentType(MediaType.APPLICATION_JSON).content(routineBody("수정한 기록 루틴", """
						[{"exerciseId":%d,"exerciseOrder":1,"targetSets":2,"targetReps":8,
						"sets":[{"setNumber":1,"weight":72.50,"reps":8},
						{"setNumber":2,"weight":75.00,"reps":6}]}]
						""".formatted(exerciseId))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/routines/{id}", firstId).with(jwtFor(owner)))
				.andExpect(jsonPath("$.exercises[0].sets[1].weight").value(75.0));
		mockMvc.perform(get("/api/workout-sessions/{id}", sessionId).with(jwtFor(owner)))
				.andExpect(jsonPath("$.routineId").isEmpty())
				.andExpect(jsonPath("$.exercises[0].sets[1].completed").value(true));
	}

	private Integer saveSessionRoutine(User user, Integer sessionId, String body) throws Exception {
		String response = mockMvc.perform(post("/api/routines/from-session/{id}", sessionId)
				.with(jwtFor(user)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private Integer createRoutine(User user, String name, String exercises) throws Exception {
		String response = mockMvc.perform(post("/api/routines")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(routineBody(name, exercises)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private void assertBadRequest(User user, String body) throws Exception {
		mockMvc.perform(post("/api/routines")
				.with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	private String routineBody(String name, String exercises) {
		return """
				{
				  "name": "%s",
				  "exercises": %s
				}
				""".formatted(name, exercises);
	}

	private String singleExercise(Integer exerciseId) {
		return """
				[{"exerciseId": %d, "exerciseOrder": 1, "targetSets": 3, "targetReps": 8}]
				""".formatted(exerciseId);
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
		jdbcTemplate.update("DELETE FROM routine_set");
		jdbcTemplate.update("DELETE FROM exercise_in_routine");
		jdbcTemplate.update("DELETE FROM routine");
		exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() != null)
				.toList());
		userRepository.deleteAll();
	}
}
