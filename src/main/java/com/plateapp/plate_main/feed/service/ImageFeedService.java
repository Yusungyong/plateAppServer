// src/main/java/com/plateapp/plate_main/feed/service/ImageFeedService.java
package com.plateapp.plate_main.feed.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.comment.repository.CommentRepository;
import com.plateapp.plate_main.comment.repository.ReplyRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.feed.dto.ImageFeedContextResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedViewerResponse;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedContextQueryRepository;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.like.repository.FeedLikeRepository;
import com.plateapp.plate_main.user.entity.Fp100User;

@Service
public class ImageFeedService {

  private final ImageFeedRepository imageFeedRepository;
  private final CommentRepository commentRepository;
  private final ReplyRepository replyRepository;
  private final FeedLikeRepository feedLikeRepository;

  // ✅ 추가
  private final ImageFeedContextQueryRepository contextQueryRepository;

  public ImageFeedService(
    ImageFeedRepository imageFeedRepository,
    CommentRepository commentRepository,
    ReplyRepository replyRepository,
    FeedLikeRepository feedLikeRepository,
    ImageFeedContextQueryRepository contextQueryRepository
  ) {
    this.imageFeedRepository = imageFeedRepository;
    this.commentRepository = commentRepository;
    this.replyRepository = replyRepository;
    this.feedLikeRepository = feedLikeRepository;
    this.contextQueryRepository = contextQueryRepository;
  }

  @Transactional(readOnly = true)
  public ImageFeedViewerResponse getViewer(Integer feedId, String username) {
    Fp400ImageFeed feed = imageFeedRepository.findByFeedIdAndUseYn(feedId, "Y")
      .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "피드를 찾을 수 없습니다."));

    Fp100User writer = feed.getWriter();

    String nickName = (writer != null) ? writer.getNickName() : null;
    String profileImageUrl = (writer != null) ? writer.getProfileImageUrl() : null;

    long commentCount = commentRepository.countActiveByFeedId(feedId);
    long replyCount = replyRepository.countActiveByFeedId(feedId);
    long totalCommentCount = commentCount + replyCount;

    long likeCount = feedLikeRepository.countByFeedIdAndUseYn(feedId, "Y");
    boolean likedByMe = false;
    if (username != null && !username.isBlank()) {
      likedByMe = feedLikeRepository.existsByUsernameAndFeedIdAndUseYnAndDeletedAtIsNull(username, feedId, "Y");
    }

    List<ImageFeedViewerResponse.ImageItem> images = parseCommaImages(feed.getImages());

    return new ImageFeedViewerResponse(
      feed.getFeedId(),
      feed.getUsername(),
      nickName,
      profileImageUrl,

      feed.getFeedTitle(),
      feed.getContent(),

      feed.getStoreName(),
      feed.getLocation(),
      feed.getPlaceId(),

      feed.getThumbnail(),

      totalCommentCount,
      likeCount,
      likedByMe,

      feed.getCreatedAt(),
      feed.getUpdatedAt(),

      images
    );
  }

  // ✅ 추가: 주변 피드ID 스트립
  @Transactional(readOnly = true)
  public ImageFeedContextResponse getContext(Integer baseFeedId, Integer radiusM, Integer limit) {
    if (baseFeedId == null || baseFeedId <= 0) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "baseFeedId가 올바르지 않습니다.");
    }

    int r = (radiusM == null) ? 3000 : radiusM;
    int l = (limit == null) ? 50 : limit;

    if (r <= 0 || r > 50000) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "radiusM 범위가 올바르지 않습니다.");
    }
    if (l <= 0 || l > 200) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "limit 범위가 올바르지 않습니다.");
    }

    ImageFeedContextQueryRepository.LatLng base = contextQueryRepository.findBaseLatLng(baseFeedId)
      .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "피드를 찾을 수 없습니다."));

    double baseLat = base.getLat();
    double baseLng = base.getLng();

    // bounding box delta (meters -> degrees)
    double latDelta = r / 111_000.0;
    double cos = Math.cos(Math.toRadians(baseLat));
    if (Math.abs(cos) < 0.01) cos = 0.01;
    double lngDelta = r / (111_000.0 * cos);

    List<Integer> feedIds = contextQueryRepository.findNearbyFeedIds(
      baseLat, baseLng, latDelta, lngDelta, r, l
    );

    if (feedIds == null || feedIds.isEmpty()) {
      feedIds = List.of(baseFeedId);
    }

    int initialIndex = feedIds.indexOf(baseFeedId);
    if (initialIndex < 0) initialIndex = 0;

    return new ImageFeedContextResponse(feedIds, initialIndex);
  }

  private List<ImageFeedViewerResponse.ImageItem> parseCommaImages(String images) {
    if (images == null || images.isBlank()) return List.of();

    String[] parts = images.split(",");
    Set<String> unique = new LinkedHashSet<>();
    for (String p : parts) {
      if (p == null) continue;
      String v = p.trim();
      if (v.isEmpty()) continue;
      unique.add(v);
    }

    List<ImageFeedViewerResponse.ImageItem> result = new ArrayList<>();
    int order = 1;
    for (String fileName : unique) {
      result.add(new ImageFeedViewerResponse.ImageItem(order++, fileName));
    }
    return result;
  }
}
