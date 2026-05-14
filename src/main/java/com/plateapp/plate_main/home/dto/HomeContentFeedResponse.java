package com.plateapp.plate_main.home.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HomeContentFeedResponse(
        String trackingToken,
        LocalDateTime generatedAt,
        List<HomeContentFeedItem> items,
        String nextCursor
) {
}
