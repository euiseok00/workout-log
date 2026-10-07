package com.workoutlog.backend.auth.dto;

public record LoginIdAvailabilityResponse(String loginId, boolean available) {
}
