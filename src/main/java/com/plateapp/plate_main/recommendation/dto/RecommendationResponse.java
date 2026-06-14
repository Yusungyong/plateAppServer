package com.plateapp.plate_main.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        String requestId,
        LocalDateTime generatedAt,
        List<RecommendationSection> sections
) {
}
