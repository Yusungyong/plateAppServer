package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.profile.dto.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;

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
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }
        try {
            String url = s3UploadService.uploadProfileImage(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            return update(username, user -> user.setProfileImageUrl(url));
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 업로드에 실패했습니다.", e);
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
        return update(username, user -> user.setFcmToken(fcmToken));
    }

    @Transactional
    public UserDetailResponse updatePrivacy(String username, Boolean isPrivate) {
        return update(username, user -> user.setIsPrivate(isPrivate));
    }

    private UserDetailResponse update(String username, Consumer<User> updater) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + username));

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
}
