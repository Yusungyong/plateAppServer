package com.plateapp.plate_main.like.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.dto.PagedResponse;
import com.plateapp.plate_main.like.dto.LikeStatusResponse;
import com.plateapp.plate_main.like.dto.LikeToggleResponse;
import com.plateapp.plate_main.like.dto.LikeUserDTO;
import com.plateapp.plate_main.like.entity.ImageFeedLike;
import com.plateapp.plate_main.like.repository.ImageFeedLikeRepository;
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
public class ImageFeedLikeService {

    private final ImageFeedLikeRepository likeRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikeToggleResponse toggleLike(Integer feedId, String username) {
        Optional<ImageFeedLike> existingLike = likeRepository.findByImageFeedIdAndUserId(feedId, username);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // 이미 좋아요 → 취소
            likeRepository.delete(existingLike.get());
            isLiked = false;
        } else {
            // 좋아요 추가
            ImageFeedLike like = ImageFeedLike.builder()
                    .imageFeedId(feedId)
                    .userId(username) // username 컬럼에 저장
                    .build();
            likeRepository.save(like);
            isLiked = true;
        }

        long likeCount = likeRepository.countByImageFeedId(feedId);

        return LikeToggleResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    @Transactional(readOnly = true)
    public LikeStatusResponse getLikeStatus(Integer feedId, String username) {
        boolean isLiked = likeRepository.existsByImageFeedIdAndUserId(feedId, username);
        long likeCount = likeRepository.countByImageFeedId(feedId);

        return LikeStatusResponse.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LikeUserDTO> getLikeUsers(Integer feedId, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<ImageFeedLike> page = likeRepository.findByImageFeedIdOrderByCreatedAtDesc(feedId, pageable);

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
