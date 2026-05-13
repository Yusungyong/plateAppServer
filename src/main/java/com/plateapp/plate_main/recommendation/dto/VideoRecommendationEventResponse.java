package com.plateapp.plate_main.recommendation.dto;

public record VideoRecommendationEventResponse(
        Long eventId,
        boolean duplicate
) {
}
