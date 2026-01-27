// src/main/java/com/plateapp/plate_main/feed/dto/ImageFeedViewerResponse.java
package com.plateapp.plate_main.feed.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImageFeedViewerResponse(
        Integer feedId,
        String username,
        String nickName,
        String profileImageUrl,

        String feedTitle,
        String content,

        String storeName,
        String location,
        String placeId,
        String groupId,

        String thumbnail,

        long commentCount,
        long likeCount,
        boolean likedByMe,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        List<ImageItem> images
) {
    public record ImageItem(Integer orderNo, String fileName) {}
}
