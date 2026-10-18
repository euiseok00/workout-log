package com.workoutlog.backend.workout;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workoutlog.backend.common.error.RequestValidationException;
import com.workoutlog.backend.exercise.Exercise;
import com.workoutlog.backend.exercise.ExerciseRepository;
import com.workoutlog.backend.routine.Routine;
import com.workoutlog.backend.routine.RoutineRepository;
import com.workoutlog.backend.workout.dto.WorkoutSessionRequest;
import com.workoutlog.backend.workout.dto.WorkoutSessionRequest.ExerciseRequest;
import com.workoutlog.backend.workout.dto.WorkoutSessionRequest.SetRequest;
import com.workoutlog.backend.workout.dto.WorkoutSessionResponse;

@Service
public class WorkoutSessionService {

	private final WorkoutSessionRepository workoutSessionRepository;
	private final RoutineRepository routineRepository;
	private final ExerciseRepository exerciseRepository;

	public WorkoutSessionService(WorkoutSessionRepository workoutSessionRepository, RoutineRepository routineRepository,
			ExerciseRepository exerciseRepository) {
		this.workoutSessionRepository = workoutSessionRepository;
		this.routineRepository = routineRepository;
		this.exerciseRepository = exerciseRepository;
	}

	@Transactional
	public WorkoutSessionResponse createSession(WorkoutSessionRequest request, Integer userId) {
		Routine routine = validateAndLoadRoutine(request.routineId(), userId);
		Map<Integer, Exercise> exercises = validateAndLoadExercises(request.exercises(), userId);
		WorkoutSession session = new WorkoutSession(userId, request.routineId(),
				routine == null ? null : routine.getName(), request.workoutDate());
		addExercises(session, request.exercises(), exercises);
		return WorkoutSessionResponse.from(workoutSessionRepository.save(session), session.getRoutineName());
	}

	@Transactional(readOnly = true)
	public List<WorkoutSessionResponse> getSessions(Integer userId, LocalDate from, LocalDate to) {
		validateDateRange(from, to);
		List<WorkoutSession> sessions = workoutSessionRepository.findAllInDateRange(userId, from, to);
		return sessions.stream()
				.map(session -> WorkoutSessionResponse.from(session, session.getRoutineName()))
				.toList();
	}

	private void validateDateRange(LocalDate from, LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new RequestValidationException("from", "시작일은 종료일보다 이후일 수 없습니다.");
		}
	}

	@Transactional(readOnly = true)
	public WorkoutSessionResponse getSession(Integer sessionId, Integer userId) {
		WorkoutSession session = findOwnedSession(sessionId, userId);
		return WorkoutSessionResponse.from(session, session.getRoutineName());
	}

	@Transactional
	public WorkoutSessionResponse updateSession(Integer sessionId, WorkoutSessionRequest request, Integer userId) {
		WorkoutSession session = findOwnedSession(sessionId, userId);
		Routine routine = request.routineId() != null && request.routineId().equals(session.getRoutineId())
				&& session.getRoutineName() != null
				? null : validateAndLoadRoutine(request.routineId(), userId);
		Map<Integer, Exercise> exercises = validateAndLoadExercises(request.exercises(), userId);
		String routineName = routine == null && request.routineId() != null
				? session.getRoutineName() : routine == null ? null : routine.getName();
		session.update(request.routineId(), routineName, request.workoutDate());
		session.clearExercises();
		workoutSessionRepository.flush();
		addExercises(session, request.exercises(), exercises);
		return WorkoutSessionResponse.from(session, session.getRoutineName());
	}

	@Transactional
	public void deleteSession(Integer sessionId, Integer userId) {
		workoutSessionRepository.delete(findOwnedSession(sessionId, userId));
	}

	private WorkoutSession findOwnedSession(Integer sessionId, Integer userId) {
		return workoutSessionRepository.findByIdAndUserId(sessionId, userId)
				.orElseThrow(WorkoutSessionNotFoundException::new);
	}

	private Routine validateAndLoadRoutine(Integer routineId, Integer userId) {
		if (routineId == null) {
			return null;
		}
		return routineRepository.findByIdAndUserId(routineId, userId)
				.orElseThrow(() -> new RequestValidationException("routineId", "사용할 수 없는 루틴입니다."));
	}

	private Map<Integer, Exercise> validateAndLoadExercises(List<ExerciseRequest> requests, Integer userId) {
		HashSet<Integer> exerciseIds = new HashSet<>();
		HashSet<Integer> exerciseOrders = new HashSet<>();
		for (ExerciseRequest request : requests) {
			exerciseIds.add(request.exerciseId());
			if (!exerciseOrders.add(request.exerciseOrder())) {
				throw new RequestValidationException("exercises", "운동 순서는 중복될 수 없습니다.");
			}
			validateSetNumbers(request.sets());
		}

		Map<Integer, Exercise> exercises = exerciseRepository.findAllById(exerciseIds).stream()
				.collect(Collectors.toMap(Exercise::getId, Function.identity()));
		for (Integer exerciseId : exerciseIds) {
			Exercise exercise = exercises.get(exerciseId);
			if (exercise == null || exercise.getUserId() != null && !exercise.getUserId().equals(userId)) {
				throw new RequestValidationException("exercises", "사용할 수 없는 운동이 포함되어 있습니다.");
			}
		}
		return exercises;
	}

	private void validateSetNumbers(List<SetRequest> sets) {
		HashSet<Integer> setNumbers = new HashSet<>();
		for (SetRequest set : sets) {
			if (!setNumbers.add(set.setNumber())) {
				throw new RequestValidationException("sets", "세트 번호는 중복될 수 없습니다.");
			}
		}
	}

	private void addExercises(WorkoutSession session, List<ExerciseRequest> requests,
			Map<Integer, Exercise> exercises) {
		for (ExerciseRequest request : requests) {
			ExerciseInSession exerciseInSession = new ExerciseInSession(session,
					exercises.get(request.exerciseId()), request.exerciseOrder());
			for (SetRequest set : request.sets()) {
				exerciseInSession.addSet(new ExerciseSet(exerciseInSession, set.setNumber(), set.weight(), set.reps(),
						set.completed()));
			}
			session.addExercise(exerciseInSession);
		}
	}
}
