package com.plateapp.plate_main.home.dto;

public record HomeContentStats(
        long likeCount,
        long commentCount,
        long viewCount,
        boolean likedByMe
) {
}
