package com.plateapp.plate_main.profile.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LikedPlaceMapResponse(
        List<LikedPlaceMapItem> items
) {
    public record LikedPlaceMapItem(
            String placeId,
            Integer storeId,
            String storeName,
            String address,
            String category,
            Double lat,
            Double lng,
            String thumbnailUrl,
            long videoLikeCount,
            long imageLikeCount,
            long totalLikeCount,
            LocalDateTime latestLikedAt
    ) {}
}
