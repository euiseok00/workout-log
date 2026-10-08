package com.workoutlog.backend.routine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
		assertBadRequest(user, routineBody("운동 중복", """
				[
				  {"exerciseId": %d, "exerciseOrder": 1},
				  {"exerciseId": %d, "exerciseOrder": 2}
				]
				""".formatted(benchPressId, benchPressId)));
		assertBadRequest(user, routineBody("순서 중복", """
				[
				  {"exerciseId": %d, "exerciseOrder": 1},
				  {"exerciseId": %d, "exerciseOrder": 1}
				]
				""".formatted(benchPressId, squatId)));
		assertBadRequest(user, routineBody("없는 운동", singleExercise(999999)));
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
		jdbcTemplate.update("DELETE FROM exercise_in_routine");
		jdbcTemplate.update("DELETE FROM routine");
		exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() != null)
				.toList());
		userRepository.deleteAll();
	}
}
