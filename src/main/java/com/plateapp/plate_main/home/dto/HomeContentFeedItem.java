package com.plateapp.plate_main.home.dto;

import java.time.LocalDateTime;

public record HomeContentFeedItem(
        String feedKey,
        String contentType,
        Integer videoFeedId,
        Integer imageFeedId,
        Integer storeId,
        String placeId,
        String title,
        String content,
        String storeName,
        String address,
        String thumbnailUrl,
        String videoUrl,
        Double aspectRatio,
        Integer durationSec,
        Integer imageCount,
        HomeContentPrimaryImage primaryImage,
        HomeContentAuthor author,
        HomeContentStats stats,
        String reason,
        Double score,
        LocalDateTime createdAt
) {
}
