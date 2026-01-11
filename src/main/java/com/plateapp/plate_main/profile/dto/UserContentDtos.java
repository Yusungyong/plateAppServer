package com.plateapp.plate_main.profile.dto;

import java.time.LocalDateTime;

public class UserContentDtos {

    public record UserVideoItem(
            Integer storeId,
            String title,
            String thumbnail,
            String fileName,
            Integer videoDuration,
            String placeId,
            String storeName,
            String address,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UserImageItem(
            Integer feedId,
            String title,
            String thumbnail,
            String placeId,
            String storeName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
