package com.plateapp.plate_main.recommendation.service;

public record RecommendationActor(
        Integer userId,
        String username,
        boolean guest,
        String guestId,
        String sessionId
) {

    public boolean present() {
        return userId != null || hasText(username) || hasText(guestId);
    }

    public static RecommendationActor none() {
        return new RecommendationActor(null, null, false, null, null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
