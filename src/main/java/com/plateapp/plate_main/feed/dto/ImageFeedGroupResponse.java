package com.plateapp.plate_main.feed.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageFeedGroupResponse(
        List<GroupItem> items,
        String nextCursor,
        boolean hasMore
) {
    public record GroupItem(
            String groupId,
            String placeId,
            String storeName,
            String address,
            Double lat,
            Double lng,
            String thumbnail,
            long imageCount,
            Integer latestFeedId,
            LocalDateTime latestCreatedAt
    ) {}
}
