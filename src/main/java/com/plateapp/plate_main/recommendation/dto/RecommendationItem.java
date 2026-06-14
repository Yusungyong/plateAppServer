package com.plateapp.plate_main.recommendation.dto;

import java.util.List;

public record RecommendationItem(
        String id,
        String surface,
        String targetType,
        String title,
        String subtitle,
        Integer storeId,
        String placeId,
        Integer feedId,
        Integer videoFeedId,
        Integer seasonalFoodId,
        String storeName,
        String address,
        String category,
        String thumbnailUrl,
        Integer distanceM,
        List<String> friendNames,
        Integer score,
        RecommendationScoreBreakdown scoreBreakdown,
        List<String> reasonLabels,
        Boolean isSeen
) {
}
