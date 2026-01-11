package com.plateapp.plate_main.profile.dto;

import java.time.LocalDate;

public record MyProfileResponse(
    Integer userId,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    LocalDate createdAt,
    Stats stats,
    Settings settings
) {
    public static MyProfileResponse of(
        Integer userId,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        LocalDate createdAt,
        Stats stats,
        Settings settings
    ) {
        return new MyProfileResponse(userId, username, email, displayName, avatarUrl, createdAt, stats, settings);
    }

    public record Stats(
        long likeCount,
        long commentCount,
        long videoPostCount,
        long imagePostCount,
        long totalPostCount
    ) {}

    public record Settings(
        boolean pushNotifications,
        boolean marketingNotifications,
        String language
    ) {}
}
