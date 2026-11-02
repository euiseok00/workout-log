package com.workoutlog.backend.statistics;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.jayway.jsonpath.JsonPath;
import com.workoutlog.backend.PostgresIntegrationTest;
import com.workoutlog.backend.exercise.BodyPart;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.auth.User;
import com.workoutlog.backend.auth.UserRepository;

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
	void calculatesDashboardAcrossFullMondayToSundayWeeks() throws Exception {
		User user = saveUser("stats_dashboard");
		User other = saveUser("dashboard_other");
		Integer chest = defaultExerciseId("벤치프레스");
		Integer back = defaultExerciseId("바벨 로우");
		Integer arms = defaultExerciseId("바벨 컬");
		Integer legs = defaultExerciseId("스쿼트");
		createSession(user, LocalDate.parse("2026-10-02"), chest, completedSet(1, "10.00", 10));
		createSession(user, LocalDate.parse("2026-09-28"), legs, completedSet(1, "10.00", 10));
		createSession(user, LocalDate.parse("2026-10-04"), chest, """
				{"setNumber":1,"weight":20.00,"reps":10,"completed":true},
				{"setNumber":2,"weight":999.00,"reps":10,"completed":false}
				""");
		createSession(user, LocalDate.parse("2026-10-05"), chest, completedSet(1, "30.00", 10));
		createSession(user, LocalDate.parse("2026-10-05"), arms, completedSet(1, "0.00", 12));
		createSession(user, LocalDate.parse("2026-10-11"), back, completedSet(1, "40.00", 5));
		createSession(other, LocalDate.parse("2026-10-11"), chest, completedSet(1, "999.00", 99));

		mockMvc.perform(get("/api/statistics/dashboard").param("today", "2026-10-11").with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyWorkoutDays").value(4))
				.andExpect(jsonPath("$.currentWeekCompletedSetCount").value(3))
				.andExpect(jsonPath("$.previousWeekCompletedSetCount").value(3))
				.andExpect(jsonPath("$.currentWeekVolume").value(500.0))
				.andExpect(jsonPath("$.previousWeekVolume").value(400.0))
				.andExpect(jsonPath("$.bodyPartVolumes.length()").value(5))
				.andExpect(jsonPath("$.bodyPartVolumes[0].bodyPart").value("CHEST"))
				.andExpect(jsonPath("$.bodyPartVolumes[0].currentWeekVolume").value(300.0))
				.andExpect(jsonPath("$.bodyPartVolumes[0].previousWeekVolume").value(300.0))
				.andExpect(jsonPath("$.bodyPartVolumes[1].currentWeekVolume").value(200.0))
				.andExpect(jsonPath("$.bodyPartVolumes[3].currentWeekVolume").value(0))
				.andExpect(jsonPath("$.bodyPartVolumes[4].previousWeekVolume").value(100.0))
				.andExpect(jsonPath("$.recentSession.workoutDate").value("2026-10-11"))
				.andExpect(jsonPath("$.recentSession.exerciseCount").value(1));

		User empty = saveUser("dashboard_empty");
		mockMvc.perform(get("/api/statistics/dashboard").param("today", "2026-10-11").with(jwtFor(empty)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyWorkoutDays").value(0))
				.andExpect(jsonPath("$.currentWeekVolume").value(0))
				.andExpect(jsonPath("$.recentSession").value((Object) null));
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

	@Test
	void comparesBodyPartVolumeAcrossAdjacentCustomPeriods() throws Exception {
		User user = saveUser("stats_period");
		User other = saveUser("stats_period_other");
		Integer bench = defaultExerciseId("벤치프레스");
		Integer squat = defaultExerciseId("스쿼트");
		LocalDate from = LocalDate.of(2026, 1, 21);
		createSession(user, from.minusDays(5), bench, completedSet(1, "10.00", 10));
		createSession(user, from.minusDays(1), bench, completedSet(1, "20.00", 10));
		createSession(user, from, bench, completedSet(1, "30.00", 10));
		createSession(user, from.plusDays(4), squat, completedSet(1, "40.00", 10));
		createSession(user, from.plusDays(5), squat, completedSet(1, "50.00", 10));
		createSession(user, from.plusDays(10), squat, completedSet(1, "60.00", 10));
		createSession(user, from.minusDays(6), bench, completedSet(1, "70.00", 10));
		createSession(other, from, bench, completedSet(1, "999.00", 10));

		mockMvc.perform(get("/api/statistics/volume-periods")
				.param("from", from.toString()).param("days", "5").with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.previous.from").value("2026-01-16"))
				.andExpect(jsonPath("$.previous.to").value("2026-01-20"))
				.andExpect(jsonPath("$.previous.totalVolume").value(300.0))
				.andExpect(jsonPath("$.selected.to").value("2026-01-25"))
				.andExpect(jsonPath("$.selected.totalVolume").value(700.0))
				.andExpect(jsonPath("$.selected.bodyParts[0].bodyPart").value("CHEST"))
				.andExpect(jsonPath("$.selected.bodyParts[0].volume").value(300.0))
				.andExpect(jsonPath("$.selected.bodyParts[4].bodyPart").value("LEGS"))
				.andExpect(jsonPath("$.selected.bodyParts[4].volume").value(400.0))
				.andExpect(jsonPath("$.next.from").value("2026-01-26"))
				.andExpect(jsonPath("$.next.totalVolume").value(500.0));

		mockMvc.perform(get("/api/statistics/volume-periods")
				.param("from", from.toString()).param("days", "0").with(jwtFor(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		mockMvc.perform(get("/api/statistics/volume-periods")
				.param("from", from.toString()).param("days", "366").with(jwtFor(user)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsRoutineHistoryForOwnerAndFollowsSessionAndRoutineDeletion() throws Exception {
		User user = saveUser("stats_routine");
		User other = saveUser("stats_routine_other");
		Integer bench = defaultExerciseId("벤치프레스");
		Integer routineId = createRoutine(user, bench);
		Integer older = createRoutineSession(user, routineId, bench, LocalDate.of(2026, 1, 1), "50.00");
		Integer newer = createRoutineSession(user, routineId, bench, LocalDate.of(2026, 1, 8), "60.00");
		createSession(user, LocalDate.of(2026, 1, 9), bench, completedSet(1, "999.00", 10));

		mockMvc.perform(get("/api/statistics/routines/{id}", routineId).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("추이 루틴"))
				.andExpect(jsonPath("$.sessionCount").value(2))
				.andExpect(jsonPath("$.totalVolume").value(1100.0))
				.andExpect(jsonPath("$.maxWeight").value(60.0))
				.andExpect(jsonPath("$.sessions[0].sessionId").value(newer))
				.andExpect(jsonPath("$.sessions[0].totalVolume").value(600.0))
				.andExpect(jsonPath("$.sessions[0].exercises[0].exerciseId").value(bench))
				.andExpect(jsonPath("$.sessions[1].sessionId").value(older));

		mockMvc.perform(get("/api/statistics/routines/{id}", routineId)
				.param("from", "2026-01-05").with(jwtFor(user)))
				.andExpect(jsonPath("$.sessionCount").value(1));
		mockMvc.perform(get("/api/statistics/routines/{id}", routineId).with(jwtFor(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));

		mockMvc.perform(delete("/api/workout-sessions/{id}", newer).with(jwtFor(user)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/statistics/routines/{id}", routineId).with(jwtFor(user)))
				.andExpect(jsonPath("$.sessionCount").value(1))
				.andExpect(jsonPath("$.totalVolume").value(500.0));

		mockMvc.perform(delete("/api/routines/{id}", routineId).with(jwtFor(user)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/statistics/routines/{id}", routineId).with(jwtFor(user)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/workout-sessions/{id}", older).with(jwtFor(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.routineName").value("추이 루틴"));
	}

	@Test
	void excludesWarmupsAndCountsDropAndGroupedSetsByTheirRules() throws Exception {
		User user = saveUser("stats_set_rules");
		Integer bench = defaultExerciseId("벤치프레스");
		Integer row = defaultExerciseId("바벨 로우");
		Integer routineId = createRoutine(user, bench);
		LocalDate date = LocalDate.of(2026, 1, 12);
		mockMvc.perform(post("/api/workout-sessions").with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"workoutDate":"%s","routineId":%d,"exercises":[
						  {"exerciseId":%d,"exerciseOrder":1,"groupNumber":1,"sets":[
						    {"setNumber":1,"weight":100.00,"reps":10,"completed":true,"setType":"WARMUP"},
						    {"setNumber":2,"weight":60.00,"reps":10,"completed":true,"setType":"SUPER"},
						    {"setNumber":3,"weight":60.00,"reps":10,"completed":true,"setType":"SUPER"},
						    {"setNumber":4,"weight":40.00,"reps":10,"completed":true,"setType":"DROP"}]},
						  {"exerciseId":%d,"exerciseOrder":2,"groupNumber":1,"sets":[
						    {"setNumber":1,"weight":50.00,"reps":10,"completed":true,"setType":"SUPER"},
						    {"setNumber":2,"weight":50.00,"reps":10,"completed":true,"setType":"SUPER"},
						    {"setNumber":3,"weight":50.00,"reps":10,"completed":false,"setType":"SUPER"}]}
						]}
						""".formatted(date, routineId, bench, row)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/statistics/summary").with(jwtFor(user)))
				.andExpect(jsonPath("$.completedSetCount").value(2))
				.andExpect(jsonPath("$.totalReps").value(50))
				.andExpect(jsonPath("$.totalVolume").value(2600.0));
		mockMvc.perform(get("/api/statistics/dashboard").param("today", date.toString()).with(jwtFor(user)))
				.andExpect(jsonPath("$.currentWeekCompletedSetCount").value(2))
				.andExpect(jsonPath("$.currentWeekVolume").value(2600.0));
		mockMvc.perform(get("/api/statistics/exercises/{id}", bench).with(jwtFor(user)))
				.andExpect(jsonPath("$.completedSetCount").value(2))
				.andExpect(jsonPath("$.totalVolume").value(1600.0))
				.andExpect(jsonPath("$.maxWeight").value(60.0));
		mockMvc.perform(get("/api/statistics/routines/{id}", routineId).with(jwtFor(user)))
				.andExpect(jsonPath("$.completedSetCount").value(2))
				.andExpect(jsonPath("$.totalVolume").value(2600.0))
				.andExpect(jsonPath("$.sessions[0].completedSetCount").value(2))
				.andExpect(jsonPath("$.sessions[0].exercises[0].completedSetCount").value(2))
				.andExpect(jsonPath("$.sessions[0].exercises[1].totalVolume").value(1000.0));
		mockMvc.perform(get("/api/statistics/volume-periods")
				.param("from", date.toString()).param("days", "1").with(jwtFor(user)))
				.andExpect(jsonPath("$.selected.totalVolume").value(2600.0));
	}

	private Integer createRoutine(User user, Integer exerciseId) throws Exception {
		String response = mockMvc.perform(post("/api/routines").with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"추이 루틴","exercises":[{"exerciseId":%d,"exerciseOrder":1,"targetSets":1,"targetReps":10}]}
						""".formatted(exerciseId)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private Integer createRoutineSession(User user, Integer routineId, Integer exerciseId, LocalDate date,
			String weight) throws Exception {
		String response = mockMvc.perform(post("/api/workout-sessions").with(jwtFor(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"workoutDate":"%s","routineId":%d,"exercises":[{"exerciseId":%d,"exerciseOrder":1,"sets":[%s]}]}
						""".formatted(date, routineId, exerciseId, completedSet(1, weight, 10))))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
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
		jdbcTemplate.update("DELETE FROM routine_set");
		jdbcTemplate.update("DELETE FROM exercise_in_routine");
		jdbcTemplate.update("DELETE FROM routine");
		exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
				.filter(exercise -> exercise.getUserId() != null)
				.toList());
		userRepository.deleteAll();
	}
}
