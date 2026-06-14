package com.plateapp.plate_main.recommendation.dto;

import java.util.List;

public record RecommendationSection(
        String key,
        String title,
        String subtitle,
        List<RecommendationItem> items
) {
}
