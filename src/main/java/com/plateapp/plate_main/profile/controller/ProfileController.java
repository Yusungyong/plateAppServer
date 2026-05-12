package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.profile.dto.ChangePasswordRequest;
import com.plateapp.plate_main.profile.dto.DeleteAccountRequest;
import com.plateapp.plate_main.profile.dto.DeleteAccountResponse;
import com.plateapp.plate_main.profile.dto.DeleteSocialAccountRequest;
import com.plateapp.plate_main.profile.dto.ProfileImageUploadResponse;
import com.plateapp.plate_main.profile.dto.UpdateProfileRequest;
import com.plateapp.plate_main.profile.dto.UserProfileDTO;
import com.plateapp.plate_main.profile.dto.UserStatsDTO;
import com.plateapp.plate_main.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        UserProfileDTO profile = profileService.getMyProfile(username);
        return ApiResponse.success(profile);
    }

    @GetMapping("/{username}")
    public ApiResponse<UserProfileDTO> getUserProfile(@PathVariable String username) {
        UserProfileDTO profile = profileService.getUserProfile(username);
        return ApiResponse.success(profile);
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        UserProfileDTO profile = profileService.updateProfile(username, request);
        return ApiResponse.success(profile);
    }

    @PostMapping("/me/push-token")
    public ApiResponse<Void> syncPushToken(
            @Valid @RequestBody SyncPushTokenRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        profileService.syncPushToken(username, request.deviceId(), request.fcmToken(), request.platform());
        return ApiResponse.success(null);
    }

    @PostMapping("/me/profile-image")
    public ApiResponse<ProfileImageUploadResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        ProfileImageUploadResponse response = profileService.uploadProfileImage(username, file);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(Authentication authentication) {
        String username = authentication.getName();
        profileService.deleteProfileImage(username);
        return ApiResponse.success(null);
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            profileService.changePassword(username, request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (ProfileService.InvalidPasswordException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("INVALID_PASSWORD", e.getMessage()));
        } catch (ProfileService.AccountUnavailableException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("USER_NOT_FOUND", e.getMessage()));
        } catch (ProfileService.UnsupportedAccountDeletionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("UNSUPPORTED_ACCOUNT_TYPE", e.getMessage()));
        }
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsDTO> getMyStats(Authentication authentication) {
        String username = authentication.getName();
        UserStatsDTO stats = profileService.getUserStats(username);
        return ApiResponse.success(stats);
    }

    @GetMapping("/{username}/stats")
    public ApiResponse<UserStatsDTO> getUserStats(@PathVariable String username) {
        UserStatsDTO stats = profileService.getUserStats(username);
        return ApiResponse.success(stats);
    }

    @DeleteMapping("/me")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            profileService.deleteAccount(username, request.password(), request.reason());
            return ResponseEntity.ok(DeleteAccountResponse.success("Account deleted successfully"));
        } catch (ProfileService.InvalidPasswordException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(DeleteAccountResponse.fail("INVALID_PASSWORD", e.getMessage()));
        } catch (ProfileService.AccountUnavailableException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DeleteAccountResponse.fail("USER_NOT_FOUND", e.getMessage()));
        } catch (ProfileService.UnsupportedAccountDeletionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DeleteAccountResponse.fail("UNSUPPORTED_ACCOUNT_TYPE", e.getMessage()));
        }
    }

    @DeleteMapping("/me/social")
    public ResponseEntity<DeleteAccountResponse> deleteSocialAccount(
            @RequestBody DeleteSocialAccountRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            profileService.deleteSocialAccount(username, request);
            return ResponseEntity.ok(DeleteAccountResponse.success("Social account deleted successfully"));
        } catch (ProfileService.InvalidSocialDeleteRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DeleteAccountResponse.fail("INVALID_REQUEST", e.getMessage()));
        } catch (ProfileService.SocialReauthenticationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(DeleteAccountResponse.fail(e.getErrorCode(), e.getMessage()));
        } catch (ProfileService.AccountUnavailableException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DeleteAccountResponse.fail("USER_NOT_FOUND", e.getMessage()));
        } catch (ProfileService.UnsupportedAccountDeletionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DeleteAccountResponse.fail("UNSUPPORTED_ACCOUNT_TYPE", e.getMessage()));
        }
    }
}
