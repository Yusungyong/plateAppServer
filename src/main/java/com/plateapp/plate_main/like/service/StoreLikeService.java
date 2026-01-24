package com.plateapp.plate_main.like.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.dto.PagedResponse;
import com.plateapp.plate_main.like.dto.LikeStatusResponse;
import com.plateapp.plate_main.like.dto.LikeToggleResponse;
import com.plateapp.plate_main.like.dto.LikeUserDTO;
import com.plateapp.plate_main.like.entity.StoreLike;
import com.plateapp.plate_main.like.repository.StoreLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreLikeService {

    private final StoreLikeRepository likeRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikeToggleResponse toggleLike(Integer storeId, String username) {
        Optional<StoreLike> existingLike = likeRepository.findByStoreIdAndUserId(storeId, username);

        boolean isLiked;
        if (existingLike.isPresent()) {
            StoreLike like = existingLike.get();
            if ("Y".equals(like.getUseYn())) {
                // 좋아요 활성 -> 비활성(소프트 삭제)
                like.setUseYn("N");
                like.setDeletedAt(java.time.LocalDateTime.now());
                likeRepository.save(like);
                isLiked = false;
            } else {
                // 좋아요 비활성 -> 재활성
                like.setUseYn("Y");
                like.setDeletedAt(null);
                likeRepository.save(like);
                isLiked = true;
            }
        } else {
            // 좋아요 신규 추가
            StoreLike like = StoreLike.builder()
                    .storeId(storeId)
                    .userId(username) // username 저장
                    .useYn("Y")
                    .build();
            likeRepository.save(like);
            isLiked = true;
        }

        long likeCount = likeRepository.countByStoreIdAndUseYn(storeId, "Y");

        return LikeToggleResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    @Transactional(readOnly = true)
    public LikeStatusResponse getLikeStatus(Integer storeId, String username) {
        boolean isLiked = likeRepository.existsByStoreIdAndUserIdAndUseYn(storeId, username, "Y");
        long likeCount = likeRepository.countByStoreIdAndUseYn(storeId, "Y");

        return LikeStatusResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LikeUserDTO> getLikeUsers(Integer storeId, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<StoreLike> page = likeRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);

        List<LikeUserDTO> users = page.getContent().stream()
                .map(like -> {
                    Optional<User> userOpt = userRepository.findById(like.getUserId());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        return LikeUserDTO.builder()
                                .userId(user.getUserId())
                                .username(user.getUsername())
                                .nickname(user.getNickname())
                                .profileImageUrl(user.getProfileImageUrl())
                                .activeRegion(user.getActiveRegion())
                                .likedAt(like.getCreatedAt())
                                .build();
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return PagedResponse.of(users, page.getTotalElements(), limit, offset);
    }
}
