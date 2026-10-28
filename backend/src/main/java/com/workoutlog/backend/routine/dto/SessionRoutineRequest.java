package com.workoutlog.backend.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionRoutineRequest(@NotBlank @Size(max = 100) String name) {
}
