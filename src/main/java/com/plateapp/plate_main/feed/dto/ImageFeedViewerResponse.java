// src/main/java/com/plateapp/plate_main/feed/dto/ImageFeedViewerResponse.java
package com.plateapp.plate_main.feed.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.plateapp.plate_main.feed.dto.ImageFeedViewerResponse.ImageItem;

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

  String thumbnail,

  long commentCount, // 댓글+대댓글 합산
  long likeCount,    // 일단 0 (fp_60 구조 확인 후 반영)

  LocalDateTime createdAt,
  LocalDateTime updatedAt,

  List<ImageItem> images
) {
  public record ImageItem(Integer orderNo, String fileName) {}
}
