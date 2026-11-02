package com.workoutlog.backend.exercise;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.workoutlog.backend.common.error.RequestValidationException;

public final class ExerciseGroups {

	private ExerciseGroups() {
	}

	public record Member(Integer groupNumber, List<SetType> setTypes) {
	}

	public static void validate(List<Member> members) {
		Map<Integer, Long> sizes = members.stream()
				.map(Member::groupNumber)
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(group -> group, Collectors.counting()));
		if (sizes.containsValue(1L)) {
			throw new RequestValidationException("exercises", "묶음에는 운동이 2개 이상 필요합니다.");
		}
		for (Member member : members) {
			int size = member.groupNumber() == null ? 0 : sizes.get(member.groupNumber()).intValue();
			for (SetType type : member.setTypes()) {
				if (type.requiresGroup() && (size == 0 || !type.fitsGroupSize(size))) {
					throw new RequestValidationException("sets",
							"슈퍼는 2개, 트라이는 3개, 자이언트는 4개 이상의 운동을 묶어야 합니다.");
				}
			}
		}
	}
}
