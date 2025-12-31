package com.plateapp.plate_main.like.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.feed.FeedGuard;
import com.plateapp.plate_main.like.dto.LikeResponses;
import com.plateapp.plate_main.like.entity.Fp60FeedLike;
import com.plateapp.plate_main.like.repository.FeedLikeRepository;

@Service
public class FeedLikeService {

  private static final String Y = "Y";

  private final FeedGuard feedGuard;
  private final FeedLikeRepository likeRepo;

  public FeedLikeService(FeedGuard feedGuard, FeedLikeRepository likeRepo) {
    this.feedGuard = feedGuard;
    this.likeRepo = likeRepo;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()
        || "anonymousUser".equals(auth.getName())) {
      throw new IllegalStateException("Unauthenticated");
    }
    return auth.getName();
  }

  @Transactional
  public LikeResponses.ToggleLikeResponse toggleLike(int feedId) {
    feedGuard.assertFeedExists(feedId);
    String username = currentUsername();

    boolean liked;

    var existing = likeRepo.findByUsernameAndFeedIdAndUseYn(username, feedId, Y);
    if (existing.isPresent()) {
      likeRepo.delete(existing.get()); // 물리삭제 = 좋아요 취소
      liked = false;
    } else {
      Fp60FeedLike like = new Fp60FeedLike();
      like.setUsername(username);
      like.setFeedId(feedId);
      try {
        likeRepo.save(like); // 좋아요 등록
        liked = true;
      } catch (DataIntegrityViolationException e) {
        // 레이스로 유니크 충돌 나면 "이미 좋아요 상태"로 간주
        liked = true;
      }
    }

    long likeCount = likeRepo.countByFeedIdAndUseYn(feedId, Y);
    return new LikeResponses.ToggleLikeResponse(liked, likeCount);
  }

  @Transactional(readOnly = true)
  public long countLikes(int feedId) {
    feedGuard.assertFeedExists(feedId);
    return likeRepo.countByFeedIdAndUseYn(feedId, Y);
  }
}
