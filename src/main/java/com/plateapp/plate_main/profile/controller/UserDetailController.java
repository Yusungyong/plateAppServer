package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.profile.dto.PublicProfileResponse;
import com.plateapp.plate_main.profile.dto.UserDetailResponse;
import com.plateapp.plate_main.profile.dto.UserUpdateRequests.*;
import com.plateapp.plate_main.profile.service.UserUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/detail")
public class UserDetailController {

    private final UserRepository userRepository;
    private final UserUpdateService userUpdateService;

    @GetMapping("/{username}")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable String username, Authentication authentication) {
        requireAdmin(authentication);
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        UserDetailResponse body = new UserDetailResponse(
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
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{username}/public-profile")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        PublicProfileResponse body = PublicProfileResponse.builder()
                .username(user.getUsername())
                .nickName(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activeRegion(user.getActiveRegion())
                .build();

        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{username}/email")
    public ResponseEntity<UserDetailResponse> updateEmail(
            @PathVariable String username,
            @RequestBody @Valid UpdateEmailRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateEmail(username, request.email()));
    }

    @PatchMapping("/{username}/phone")
    public ResponseEntity<UserDetailResponse> updatePhone(
            @PathVariable String username,
            @RequestBody @Valid UpdatePhoneRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updatePhone(username, request.phone()));
    }

    @PatchMapping("/{username}/role")
    public ResponseEntity<UserDetailResponse> updateRole(
            @PathVariable String username,
            @RequestBody @Valid UpdateRoleRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateRole(username, request.role()));
    }

    @PatchMapping("/{username}/active-region")
    public ResponseEntity<UserDetailResponse> updateActiveRegion(
            @PathVariable String username,
            @RequestBody @Valid UpdateActiveRegionRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateActiveRegion(username, request.activeRegion()));
    }

    @PatchMapping("/{username}/profile-image")
    public ResponseEntity<UserDetailResponse> updateProfileImage(
            @PathVariable String username,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.uploadAndUpdateProfileImage(username, file));
    }

    @PatchMapping("/{username}/nickname")
    public ResponseEntity<UserDetailResponse> updateNickName(
            @PathVariable String username,
            @RequestBody @Valid UpdateNickNameRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateNickName(username, request.nickName()));
    }

    @PatchMapping("/{username}/code")
    public ResponseEntity<UserDetailResponse> updateCode(
            @PathVariable String username,
            @RequestBody @Valid UpdateCodeRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateCode(username, request.code()));
    }

    @PatchMapping("/{username}/fcm-token")
    public ResponseEntity<UserDetailResponse> updateFcmToken(
            @PathVariable String username,
            @RequestBody @Valid UpdateFcmTokenRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updateFcmToken(username, request.fcmToken()));
    }

    @PatchMapping("/{username}/privacy")
    public ResponseEntity<UserDetailResponse> updatePrivacy(
            @PathVariable String username,
            @RequestBody @Valid UpdatePrivacyRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(userUpdateService.updatePrivacy(username, request.isPrivate()));
    }

    private void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new AccessDeniedException("Admin role required");
        }
    }
}
