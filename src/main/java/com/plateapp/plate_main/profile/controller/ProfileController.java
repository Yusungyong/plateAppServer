package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.profile.dto.*;
import com.plateapp.plate_main.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 내 프로필 조회
    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileDTO profile = profileService.getMyProfile(username);
        return ApiResponse.success(profile);
    }

    // 다른 사용자 프로필 조회
    @GetMapping("/{username}")
    public ApiResponse<UserProfileDTO> getUserProfile(@PathVariable String username) {
        UserProfileDTO profile = profileService.getUserProfile(username);
        return ApiResponse.success(profile);
    }


    // 프로필 수정
    @PutMapping("/me")
    public ApiResponse<UserProfileDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        UserProfileDTO profile = profileService.updateProfile(username, request);
        return ApiResponse.success(profile);
    }

    // 프로필 이미지 업로드
    @PostMapping("/me/profile-image")
    public ApiResponse<ProfileImageUploadResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        ProfileImageUploadResponse response = profileService.uploadProfileImage(username, file);
        return ApiResponse.success(response);
    }

    // 프로필 이미지 삭제
    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(Authentication authentication) {
        String username = authentication.getName();
        profileService.deleteProfileImage(username);
        return ApiResponse.success(null);
    }

    // 비밀번호 변경
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        profileService.changePassword(username, request);
        return ApiResponse.success(null);
    }

    // 사용자 통계 조회
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsDTO> getMyStats(Authentication authentication) {
        String username = authentication.getName();
        UserStatsDTO stats = profileService.getUserStats(username);
        return ApiResponse.success(stats);
    }

    // 다른 사용자 통계 조회
    @GetMapping("/{username}/stats")
    public ApiResponse<UserStatsDTO> getUserStats(@PathVariable String username) {
        UserStatsDTO stats = profileService.getUserStats(username);
        return ApiResponse.success(stats);
    }

    // 계정 삭제
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String username = authentication.getName();
        String password = request.get("password");
        profileService.deleteAccount(username, password);
        return ApiResponse.success(null);
    }
}
