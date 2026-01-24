package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.profile.dto.*;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final Fp150FriendRepository friendRepository;
    private final ImageFeedRepository imageFeedRepository;
    private final Fp300StoreRepository storeRepository;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toProfileDTO(user, true);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toProfileDTO(user, false);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return PublicProfileResponse.builder()
                .username(user.getUsername())
                .nickName(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activeRegion(user.getActiveRegion())
                .build();
    }

    @Transactional
    public UserProfileDTO updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getActiveRegion() != null) {
            user.setActiveRegion(request.getActiveRegion());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhone(request.getPhoneNumber());
        }

        User updated = userRepository.save(user);
        return toProfileDTO(updated, true);
    }

    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(String username, MultipartFile file) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            // S3에 업로드
            String imageUrl = s3UploadService.uploadProfileImage(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            // 기존 이미지 삭제 (선택적)
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                // s3UploadService.deleteFile(user.getProfileImageUrl());
            }

            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return new ProfileImageUploadResponse(imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    @Transactional
    public void deleteProfileImage(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            // S3에서 이미지 삭제
            // s3UploadService.deleteFile(user.getProfileImageUrl());
        }

        user.setProfileImageUrl(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        // TODO: Implement password change with PasswordEncoder
        throw new UnsupportedOperationException("Password change is not yet implemented. Please configure PasswordEncoder bean.");
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        long friendsCount = friendRepository.countByUsernameAndStatus(username, "accepted");
        long postsCount = imageFeedRepository.countByUsernameAndUseYn(username, "Y") +
                          storeRepository.countByUsernameAndUseYn(username, "Y");

        // 받은 좋아요 총합 (간단히 이미지 피드만 계산)
        long likesCount = 0; // TODO: 구현 필요

        long visitedStoresCount = storeRepository.countByUsernameAndUseYn(username, "Y");

        return UserStatsDTO.builder()
                .friendsCount(friendsCount)
                .postsCount(postsCount)
                .likesCount(likesCount)
                .visitedStoresCount(visitedStoresCount)
                .build();
    }

    @Transactional
    public void deleteAccount(String username, String password) {
        // TODO: Implement account deletion with password verification
        throw new UnsupportedOperationException("Account deletion is not yet implemented. Please configure PasswordEncoder bean.");
    }

    private UserProfileDTO toProfileDTO(User user, boolean includePrivateInfo) {
        UserProfileDTO.UserProfileDTOBuilder builder = UserProfileDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activeRegion(user.getActiveRegion())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (includePrivateInfo) {
            builder.email(user.getEmail());
            builder.phoneNumber(user.getPhone());
        }

        return builder.build();
    }
}
