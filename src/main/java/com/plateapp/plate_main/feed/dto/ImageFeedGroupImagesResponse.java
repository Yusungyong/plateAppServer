package com.plateapp.plate_main.feed.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageFeedGroupImagesResponse(
        List<ImageItem> items,
        String nextCursor,
        boolean hasMore
) {
    public record ImageItem(
            Integer feedId,
            String fileName,
            LocalDateTime createdAt,
            String username,
            String nickName
    ) {}
}
