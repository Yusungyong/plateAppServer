package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.profile.dto.UserDetailResponse;
import com.plateapp.plate_main.profile.dto.UserUpdateRequests.*;
import com.plateapp.plate_main.profile.service.UserUpdateService;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserDetailController {

    private final UserRepository userRepository;
    private final UserUpdateService userUpdateService;

    @GetMapping("/{username}")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + username));

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

    @PatchMapping("/{username}/email")
    public ResponseEntity<UserDetailResponse> updateEmail(
            @PathVariable String username,
            @RequestBody @Valid UpdateEmailRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateEmail(username, request.email()));
    }

    @PatchMapping("/{username}/phone")
    public ResponseEntity<UserDetailResponse> updatePhone(
            @PathVariable String username,
            @RequestBody @Valid UpdatePhoneRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updatePhone(username, request.phone()));
    }

    @PatchMapping("/{username}/role")
    public ResponseEntity<UserDetailResponse> updateRole(
            @PathVariable String username,
            @RequestBody @Valid UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateRole(username, request.role()));
    }

    @PatchMapping("/{username}/active-region")
    public ResponseEntity<UserDetailResponse> updateActiveRegion(
            @PathVariable String username,
            @RequestBody @Valid UpdateActiveRegionRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateActiveRegion(username, request.activeRegion()));
    }

    @PatchMapping("/{username}/profile-image")
    public ResponseEntity<UserDetailResponse> updateProfileImage(
            @PathVariable String username,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(userUpdateService.uploadAndUpdateProfileImage(username, file));
    }

    @PatchMapping("/{username}/nickname")
    public ResponseEntity<UserDetailResponse> updateNickName(
            @PathVariable String username,
            @RequestBody @Valid UpdateNickNameRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateNickName(username, request.nickName()));
    }

    @PatchMapping("/{username}/code")
    public ResponseEntity<UserDetailResponse> updateCode(
            @PathVariable String username,
            @RequestBody @Valid UpdateCodeRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateCode(username, request.code()));
    }

    @PatchMapping("/{username}/fcm-token")
    public ResponseEntity<UserDetailResponse> updateFcmToken(
            @PathVariable String username,
            @RequestBody @Valid UpdateFcmTokenRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updateFcmToken(username, request.fcmToken()));
    }

    @PatchMapping("/{username}/privacy")
    public ResponseEntity<UserDetailResponse> updatePrivacy(
            @PathVariable String username,
            @RequestBody @Valid UpdatePrivacyRequest request
    ) {
        return ResponseEntity.ok(userUpdateService.updatePrivacy(username, request.isPrivate()));
    }
}
