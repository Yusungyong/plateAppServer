package com.plateapp.plate_main.like.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.like.dto.LikeUserResponse;
import com.plateapp.plate_main.like.entity.Fp50Like;
import com.plateapp.plate_main.like.entity.Fp50LikeId;
import com.plateapp.plate_main.like.repository.Fp50LikeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {

  private static final String USE_Y = "Y";

  private final Fp50LikeRepository likeRepository;

  @Transactional
  public boolean like(String username, Integer storeId) {
    Fp50Like like = likeRepository
        .findById(new Fp50LikeId(username, storeId))
        .orElseGet(() -> Fp50Like.builder()
            .id(new Fp50LikeId(username, storeId))
            .useYn(USE_Y)
            .build());

    like.activate();
    likeRepository.save(like);
    return true;
  }

  @Transactional
  public boolean unlike(String username, Integer storeId) {
    Fp50Like like = likeRepository
        .findById(new Fp50LikeId(username, storeId))
        .orElse(null);

    if (like == null) return false;

    like.deactivate();
    likeRepository.save(like);
    return true;
  }

  @Transactional(readOnly = true)
  public boolean isLiked(String username, Integer storeId) {
    return likeRepository.existsByIdUsernameAndIdStoreIdAndUseYn(username, storeId, USE_Y);
  }

  @Transactional(readOnly = true)
  public long countLikes(Integer storeId) {
    return likeRepository.countByIdStoreIdAndUseYn(storeId, USE_Y);
  }

  @Transactional(readOnly = true)
  public List<Integer> myLikedStoreIds(String username) {
    return likeRepository.findByIdUsernameAndUseYn(username, USE_Y)
        .stream()
        .map(l -> l.getId().getStoreId())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Map<Integer, Long> getLikeCountMap(List<Integer> storeIds) {
    if (storeIds == null || storeIds.isEmpty()) return Collections.emptyMap();

    return likeRepository.countActiveByStoreIds(storeIds).stream()
        .collect(Collectors.toMap(
            Fp50LikeRepository.StoreLikeCount::getStoreId,
            Fp50LikeRepository.StoreLikeCount::getCnt
        ));
  }

  @Transactional(readOnly = true)
  public Set<Integer> getMyLikedStoreIdSet(String username, List<Integer> storeIds) {
    if (username == null || username.isBlank() || storeIds == null || storeIds.isEmpty()) return Collections.emptySet();

    return new HashSet<>(likeRepository.findMyActiveLikedStoreIds(username, storeIds));
  }

  @Transactional(readOnly = true)
  public List<LikeUserResponse> findLikeUsers(Integer storeId, int limit, int offset) {
    int safeLimit = Math.min(Math.max(limit, 1), 100);
    int safeOffset = Math.max(offset, 0);

    return likeRepository.findActiveLikeUsers(storeId, safeLimit, safeOffset).stream()
            .map(row -> LikeUserResponse.builder()
                    .userId(row.getUserId())
                    .username(row.getUsername())
                    .nickname(row.getNickname())
                    .profileImageUrl(row.getProfileImageUrl())
                    .activeRegion(row.getActiveRegion())
                    .likedAt(row.getLikedAt() != null ? row.getLikedAt().toLocalDateTime() : null)
                    .build())
            .toList();
  }
}
