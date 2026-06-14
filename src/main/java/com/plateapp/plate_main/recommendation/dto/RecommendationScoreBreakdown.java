package com.plateapp.plate_main.recommendation.dto;

public record RecommendationScoreBreakdown(
        Integer nearby,
        Integer categoryAffinity,
        Integer friendSignal,
        Integer popularity,
        Integer seasonal,
        Integer similarity,
        Integer seenPenalty
) {
}
