package com.plateapp.plate_main.profile.dto;

import java.time.LocalDateTime;

public class LikedContentDtos {

    public record LikedVideoItem(
            Integer storeId,
            String title,
            String thumbnail,
            String fileName,
            Integer videoDuration,
            String placeId,
            String storeName,
            String address,
            LocalDateTime likedAt
    ) {}

    public record LikedImageItem(
            Integer feedId,
            String title,
            String thumbnail,
            String placeId,
            String storeName,
            LocalDateTime likedAt
    ) {}
}
