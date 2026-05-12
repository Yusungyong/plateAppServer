package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.notification.service.UserPushTokenService;
import com.plateapp.plate_main.profile.dto.UserDetailResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final ImageProcessingService imageProcessingService;
    private final UserPushTokenService userPushTokenService;

    @Transactional
    public UserDetailResponse updateEmail(String username, String email) {
        return update(username, user -> user.setEmail(email));
    }

    @Transactional
    public UserDetailResponse updatePhone(String username, String phone) {
        return update(username, user -> user.setPhone(phone));
    }

    @Transactional
    public UserDetailResponse updateRole(String username, String role) {
        return update(username, user -> user.setRole(role));
    }

    @Transactional
    public UserDetailResponse updateActiveRegion(String username, String activeRegion) {
        return update(username, user -> user.setActiveRegion(activeRegion));
    }

    @Transactional
    public UserDetailResponse updateProfileImage(String username, String profileImageUrl) {
        return update(username, user -> user.setProfileImageUrl(profileImageUrl));
    }

    @Transactional
    public UserDetailResponse uploadAndUpdateProfileImage(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty.");
        }

        Path sourcePath = null;
        Path optimizedPath = null;
        try {
            sourcePath = Files.createTempFile("profile-admin-upload-source-", ".bin");
            file.transferTo(sourcePath);
            optimizedPath = imageProcessingService.resizeMaxToTempFile(sourcePath, 1280, 1280, "jpg");

            String url;
            try (InputStream inputStream = Files.newInputStream(optimizedPath)) {
                url = s3UploadService.uploadProfileImage(
                        toJpgFileName(file.getOriginalFilename(), "profile.jpg"),
                        inputStream,
                        Files.size(optimizedPath),
                        "image/jpeg"
                );
            }

            return update(username, user -> user.setProfileImageUrl(url));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload profile image.", e);
        } finally {
            deleteTempFile(sourcePath);
            deleteTempFile(optimizedPath);
        }
    }

    @Transactional
    public UserDetailResponse updateNickName(String username, String nickName) {
        return update(username, user -> user.setNickname(nickName));
    }

    @Transactional
    public UserDetailResponse updateCode(String username, String code) {
        return update(username, user -> user.setCode(code));
    }

    @Transactional
    public UserDetailResponse updateFcmToken(String username, String fcmToken) {
        return update(username, user -> {
            user.setFcmToken(fcmToken);
            userPushTokenService.upsertLegacyToken(user, fcmToken);
        });
    }

    @Transactional
    public UserDetailResponse updatePrivacy(String username, Boolean isPrivate) {
        return update(username, user -> user.setIsPrivate(isPrivate));
    }

    private UserDetailResponse update(String username, Consumer<User> updater) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        updater.accept(user);
        user.setUpdatedAt(LocalDate.now());
        userRepository.save(user);

        return mapToResponse(user);
    }

    private UserDetailResponse mapToResponse(User user) {
        return new UserDetailResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getActiveRegion(),
                user.getProfileImageUrl(),
                user.getNickname(),
                user.getCode(),
                user.getFcmToken(),
                user.getIsPrivate()
        );
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
        } catch (IOException ignored) {
            // Cleanup failure should not mask upload result.
        }
    }
}
