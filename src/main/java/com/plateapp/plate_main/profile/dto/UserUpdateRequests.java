package com.plateapp.plate_main.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserUpdateRequests {
    public record UpdateEmailRequest(
            @NotBlank(message = "email is required")
            @Email(message = "email format is invalid")
            String email
    ) {}

    public record UpdatePhoneRequest(
            @NotBlank(message = "phone is required")
            String phone
    ) {}

    public record UpdateRoleRequest(
            @NotBlank(message = "role is required")
            String role
    ) {}

    public record UpdateActiveRegionRequest(String activeRegion) {}

    public record UpdateProfileImageRequest(String profileImageUrl) {}

    public record UpdateNickNameRequest(String nickName) {}

    public record UpdateCodeRequest(String code) {}

    public record UpdateFcmTokenRequest(String fcmToken) {}

    public record SyncPushTokenRequest(
            @NotBlank(message = "deviceId is required")
            String deviceId,
            @NotBlank(message = "fcmToken is required")
            String fcmToken,
            String platform
    ) {}

    public record UpdatePrivacyRequest(
            @NotNull(message = "isPrivate is required")
            Boolean isPrivate
    ) {}
}
