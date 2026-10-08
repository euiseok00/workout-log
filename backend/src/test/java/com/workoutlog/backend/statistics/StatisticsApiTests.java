package com.workoutlog.backend.statistics;

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
import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.user.User;
import com.workoutlog.backend.user.UserRepository;

class StatisticsApiTests extends PostgresIntegrationTest {

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
	void filtersWorkoutSessionsByOptionalDateRangeAndRejectsInvalidRanges() throws Exception {
		User user = saveUser("stats_range");
		Integer exerciseId = defaultExerciseId("벤치프레스");
		LocalDate today = LocalDate.now();
		createSession(user, today.minusDays(2), exerciseId, completedSet(1, "10.00", 10));
		createSession(user, today.minusDays(1), exerciseId, completedSet(1, "20.00", 10));
		createSession(user, today, exerciseId, completedSet(1, "30.00", 10));

		mockMvc.perform(get("/api/workout-sessions").param("from", today.minusDays(1).toString()).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].workoutDate").value(today.toString()));

		mockMvc.perform(get("/api/workout-sessions").param("to", today.minusDays(1).toString()).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		mockMvc.perform(get("/api/workout-sessions")
				.param("from", today.minusDays(1).toString())
				.param("to", today.minusDays(1).toString())
				.with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].workoutDate").value(today.minusDays(1).toString()));

		mockMvc.perform(get("/api/workout-sessions")
				.param("from", today.toString())
				.param("to", today.minusDays(1).toString())
				.with(jwtFor(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		mockMvc.perform(get("/api/workout-sessions").param("from", "not-a-date").with(jwtFor(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void calculatesSummaryFromCompletedSetsForCurrentUserOnly() throws Exception {
		User user = saveUser("stats_summary");
		User other = saveUser("stats_summary_other");
		Integer exerciseId = defaultExerciseId("스쿼트");
		LocalDate today = LocalDate.now();
		createSession(user, today.minusDays(1), exerciseId, """
				{"setNumber":1,"weight":60.00,"reps":10,"completed":true},
				{"setNumber":2,"weight":100.00,"reps":99,"completed":false},
				{"setNumber":3,"weight":70.25,"reps":8,"completed":true}
				""");
		createSession(user, today, exerciseId, completedSet(1, "0.00", 12));
		createSession(other, today, exerciseId, completedSet(1, "999.00", 99));

		mockMvc.perform(get("/api/statistics/summary").with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workoutCount").value(2))
				.andExpect(jsonPath("$.completedSetCount").value(3))
				.andExpect(jsonPath("$.totalReps").value(30))
				.andExpect(jsonPath("$.totalVolume").value(1162.0));

		mockMvc.perform(get("/api/statistics/summary")
				.param("from", today.toString())
				.param("to", today.toString())
				.with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workoutCount").value(1))
				.andExpect(jsonPath("$.totalReps").value(12))
				.andExpect(jsonPath("$.totalVolume").value(0));
	}

	@Test
	void returnsExerciseStatisticsAndAggregatesHistoryByDate() throws Exception {
		User user = saveUser("stats_exercise");
		User other = saveUser("stats_exercise_other");
		Integer exerciseId = defaultExerciseId("벤치프레스");
		LocalDate today = LocalDate.now();
		LocalDate older = today.minusDays(1);
		createSession(user, today, exerciseId, """
				{"setNumber":1,"weight":60.00,"reps":10,"completed":true},
				{"setNumber":2,"weight":100.00,"reps":1,"completed":false}
				""");
		createSession(user, today, exerciseId, completedSet(1, "65.00", 8));
		createSession(user, older, exerciseId, completedSet(1, "50.00", 12));
		createSession(other, today, exerciseId, completedSet(1, "200.00", 20));

		mockMvc.perform(get("/api/statistics/exercises/{exerciseId}", exerciseId).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exerciseId").value(exerciseId))
				.andExpect(jsonPath("$.name").value("벤치프레스"))
				.andExpect(jsonPath("$.bodyPart").value("CHEST"))
				.andExpect(jsonPath("$.sessionCount").value(3))
				.andExpect(jsonPath("$.completedSetCount").value(3))
				.andExpect(jsonPath("$.totalReps").value(30))
				.andExpect(jsonPath("$.totalVolume").value(1720.0))
				.andExpect(jsonPath("$.maxWeight").value(65.0))
				.andExpect(jsonPath("$.history.length()").value(2))
				.andExpect(jsonPath("$.history[0].workoutDate").value(today.toString()))
				.andExpect(jsonPath("$.history[0].completedSetCount").value(2))
				.andExpect(jsonPath("$.history[0].totalReps").value(18))
				.andExpect(jsonPath("$.history[0].totalVolume").value(1120.0))
				.andExpect(jsonPath("$.history[0].maxWeight").value(65.0))
				.andExpect(jsonPath("$.history[1].workoutDate").value(older.toString()));

		Exercise privateExercise = exerciseRepository.save(new Exercise("타인 전용", BodyPart.ARMS, other.getId()));
		mockMvc.perform(get("/api/statistics/exercises/{exerciseId}", privateExercise.getId()).with(jwtFor(user)))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsReversedStatisticsDateRange() throws Exception {
		User user = saveUser("stats_invalid");
		LocalDate today = LocalDate.now();

		mockMvc.perform(get("/api/statistics/summary")
				.param("from", today.toString())
				.param("to", today.minusDays(1).toString())
				.with(jwtFor(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
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

	private String completedSet(int setNumber, String weight, int reps) {
		return """
				{"setNumber":%d,"weight":%s,"reps":%d,"completed":true}
				""".formatted(setNumber, weight, reps);
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
