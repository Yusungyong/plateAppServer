package com.plateapp.plate_main.profile.dto;

import java.time.LocalDateTime;

public class ProfileActivityDetailItems {

    public record VideoItem(
            Integer storeId,
            String title,
            String thumbnail,
            String fileName,
            Integer videoDuration,
            String placeId,
            String storeName,
            String address,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long likeCount,
            Long commentCount
    ) {}

    public record ImageItem(
            Integer feedId,
            String title,
            String thumbnail,
            String placeId,
            String storeName,
            String address,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer imageCount,
            Long likeCount,
            Long commentCount
    ) {}
}
