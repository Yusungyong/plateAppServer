package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.domain.SocialAccount;
import com.plateapp.plate_main.auth.dto.ProfileHistoryRequest;
import com.plateapp.plate_main.auth.service.ProfileHistoryService;
import com.plateapp.plate_main.auth.service.SocialAuthService;
import com.plateapp.plate_main.auth.repository.RefreshTokenRepository;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.notification.service.UserPushTokenService;
import com.plateapp.plate_main.profile.dto.ChangePasswordRequest;
import com.plateapp.plate_main.profile.dto.DeleteSocialAccountRequest;
import com.plateapp.plate_main.profile.dto.ProfileImageUploadResponse;
import com.plateapp.plate_main.profile.dto.PublicProfileResponse;
import com.plateapp.plate_main.profile.dto.UpdateProfileRequest;
import com.plateapp.plate_main.profile.dto.UserProfileDTO;
import com.plateapp.plate_main.profile.dto.UserStatsDTO;
import com.plateapp.plate_main.video.repository.Fp305WatchHistoryRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import lombok.RequiredArgsConstructor;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final Fp305WatchHistoryRepository watchHistoryRepository;
    private final Fp440CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final S3UploadService s3UploadService;
    private final ImageProcessingService imageProcessingService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ProfileHistoryService profileHistoryService;
    private final SocialAuthService socialAuthService;
    private final UserPushTokenService userPushTokenService;

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

        boolean changed = false;

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
            changed = true;
        }
        if (request.getActiveRegion() != null) {
            user.setActiveRegion(request.getActiveRegion());
            changed = true;
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
            changed = true;
        }
        if (request.getPhoneNumber() != null) {
            user.setPhone(request.getPhoneNumber());
            changed = true;
        }
        if (request.getFcmToken() != null) {
            userPushTokenService.upsertLegacyToken(user, request.getFcmToken());
            changed = true;
        }
        if (changed) {
            user.setUpdatedAt(java.time.LocalDate.now());
        }

        User updated = userRepository.save(user);
        return toProfileDTO(updated, true);
    }

    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(String username, MultipartFile file) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Path sourcePath = null;
        Path optimizedPath = null;
        try {
            sourcePath = Files.createTempFile("profile-upload-source-", ".bin");
            file.transferTo(sourcePath);
            optimizedPath = imageProcessingService.resizeMaxToTempFile(sourcePath, 1280, 1280, "jpg");

            String imageUrl;
            try (InputStream inputStream = Files.newInputStream(optimizedPath)) {
                imageUrl = s3UploadService.uploadProfileImage(
                        toJpgFileName(file.getOriginalFilename(), "profile.jpg"),
                        inputStream,
                        Files.size(optimizedPath),
                        "image/jpeg"
                );
            }

            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return new ProfileImageUploadResponse(imageUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image", e);
        } finally {
            deleteTempFile(sourcePath);
            deleteTempFile(optimizedPath);
        }
    }

    @Transactional
    public void deleteProfileImage(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            s3UploadService.deleteObjectByUrl(user.getProfileImageUrl());
        }

        user.setProfileImageUrl(null);
        userRepository.save(user);
    }

    @Transactional
    public void syncPushToken(String username, String deviceId, String fcmToken, String platform) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userPushTokenService.upsertAppToken(user, deviceId, platform, fcmToken);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new InvalidPasswordException("Current password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new InvalidPasswordException("New password is required");
        }

        User user = userRepository.findById(username)
                .orElseThrow(() -> new AccountUnavailableException("User not found"));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new UnsupportedAccountDeletionException("Password change is only available for normal accounts");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password does not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDate.now());
        userRepository.save(user);

        profileHistoryService.record(
                username,
                ProfileHistoryRequest.builder()
                        .changeType("CD_003")
                        .before(Map.of("passwordChanged", false))
                        .after(Map.of("passwordChanged", true, "changeMethod", "authenticated_user"))
                        .build()
        );

        refreshTokenRepository.deleteByUsername(username);
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
        long likesCount = 0;
        long visitedStoresCount = storeRepository.countByUsernameAndUseYn(username, "Y");

        return UserStatsDTO.builder()
                .friendsCount(friendsCount)
                .postsCount(postsCount)
                .likesCount(likesCount)
                .visitedStoresCount(visitedStoresCount)
                .build();
    }

    @Transactional
    public void deleteAccount(String username, String password, String reason) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("Password is required");
        }

        User user = userRepository.findById(username)
                .orElseThrow(() -> new AccountUnavailableException("User not found"));

        Integer userId = user.getUserId();
        if (userId != null && socialAccountRepository.existsByUserId(userId)) {
            throw new UnsupportedAccountDeletionException("Use social account deletion API for social accounts");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new UnsupportedAccountDeletionException("Password-based account deletion is only available for normal accounts");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException("Password does not match");
        }

        profileHistoryService.record(
                username,
                ProfileHistoryRequest.builder()
                        .changeType("CD_007")
                        .before(buildDeleteBeforeSnapshot(user))
                        .after(buildDeleteAfterSnapshot(user, reason))
                        .memo(buildDeleteMemo(reason))
                        .build()
        );

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            s3UploadService.deleteObjectByUrl(user.getProfileImageUrl());
        }

        detachUserIdReferences(userId);
        refreshTokenRepository.deleteByUsername(username);
        userRepository.delete(user);
    }

    @Transactional
    public void deleteSocialAccount(String username, DeleteSocialAccountRequest request) {
        if (request == null || request.provider() == null || request.provider().isBlank()) {
            throw new InvalidSocialDeleteRequestException("provider is required");
        }

        User user = userRepository.findById(username)
                .orElseThrow(() -> new AccountUnavailableException("User not found"));

        Integer userId = user.getUserId();
        if (userId == null) {
            throw new AccountUnavailableException("User not found");
        }

        SocialAccount socialAccount = socialAccountRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new UnsupportedAccountDeletionException("Current account is not a social account"));

        String provider = normalizeProvider(request.provider());
        if (!socialAccount.getProvider().equalsIgnoreCase(provider)) {
            throw new SocialReauthenticationException("Provider does not match current social account", "SOCIAL_PROVIDER_MISMATCH");
        }

        String providerUserId = verifySocialDeleteRequest(provider, request);
        if (!socialAccount.getProviderUserId().equals(providerUserId)) {
            throw new SocialReauthenticationException("Social re-authentication failed", "SOCIAL_REAUTH_FAILED");
        }

        profileHistoryService.record(
                username,
                ProfileHistoryRequest.builder()
                        .changeType("CD_007")
                        .before(buildDeleteBeforeSnapshot(user))
                        .after(buildSocialDeleteAfterSnapshot(user, socialAccount, request.reason()))
                        .memo(buildSocialDeleteMemo(provider, request.reason()))
                        .build()
        );

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            s3UploadService.deleteObjectByUrl(user.getProfileImageUrl());
        }

        detachUserIdReferences(userId);
        refreshTokenRepository.deleteByUsername(username);
        socialAccountRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private void detachUserIdReferences(Integer userId) {
        if (userId == null) {
            return;
        }
        commentRepository.clearUserIdByUserId(userId);
        watchHistoryRepository.clearUserIdByUserId(userId);
    }

    private Map<String, Object> buildDeleteBeforeSnapshot(User user) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("username", user.getUsername());
        before.put("email", user.getEmail());
        before.put("phone", user.getPhone());
        before.put("role", user.getRole());
        before.put("activeRegion", user.getActiveRegion());
        before.put("profileImageUrl", user.getProfileImageUrl());
        before.put("nickname", user.getNickname());
        before.put("code", user.getCode());
        before.put("isPrivate", user.getIsPrivate());
        return before;
    }

    private Map<String, Object> buildDeleteAfterSnapshot(User user, String reason) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("username", user.getUsername());
        after.put("deleted", true);
        after.put("status", "deleted");
        if (reason != null && !reason.isBlank()) {
            after.put("reason", reason.trim());
        }
        return after;
    }

    private Map<String, Object> buildSocialDeleteAfterSnapshot(User user, SocialAccount socialAccount, String reason) {
        Map<String, Object> after = buildDeleteAfterSnapshot(user, reason);
        after.put("provider", normalizeProvider(socialAccount.getProvider()));
        after.put("socialDeleted", true);
        return after;
    }

    private String buildDeleteMemo(String reason) {
        if (reason == null || reason.isBlank()) {
            return "normal account delete";
        }
        return "normal account delete: " + reason.trim();
    }

    private String buildSocialDeleteMemo(String provider, String reason) {
        if (reason == null || reason.isBlank()) {
            return "social account delete: " + provider;
        }
        return "social account delete: " + provider + " - " + reason.trim();
    }

    private String verifySocialDeleteRequest(String provider, DeleteSocialAccountRequest request) {
        try {
            return switch (provider) {
                case "APPLE" -> socialAuthService.verifyAppleReauthentication(request.identityToken());
                case "GOOGLE" -> socialAuthService.verifyGoogleReauthentication(request.idToken());
                case "KAKAO" -> socialAuthService.verifyKakaoReauthentication(request.accessToken());
                default -> throw new InvalidSocialDeleteRequestException("Unsupported provider");
            };
        } catch (InvalidSocialDeleteRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new SocialReauthenticationException("Social re-authentication failed", "SOCIAL_REAUTH_FAILED");
        }
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase();
    }

    private String toJpgFileName(String originalName, String fallback) {
        String safeName = (originalName == null || originalName.isBlank()) ? fallback : originalName;
        int dot = safeName.lastIndexOf('.');
        String baseName = dot > 0 ? safeName.substring(0, dot) : safeName;
        return baseName + ".jpg";
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // Cleanup failure should not mask upload success/failure.
        }
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

    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException(String message) {
            super(message);
        }
    }

    public static class AccountUnavailableException extends RuntimeException {
        public AccountUnavailableException(String message) {
            super(message);
        }
    }

    public static class UnsupportedAccountDeletionException extends RuntimeException {
        public UnsupportedAccountDeletionException(String message) {
            super(message);
        }
    }

    public static class InvalidSocialDeleteRequestException extends RuntimeException {
        public InvalidSocialDeleteRequestException(String message) {
            super(message);
        }
    }

    public static class SocialReauthenticationException extends RuntimeException {
        private final String errorCode;

        public SocialReauthenticationException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
