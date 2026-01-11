package com.plateapp.plate_main.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserUpdateRequests {
    public record UpdateEmailRequest(
            @NotBlank(message = "email은 필수입니다.")
            @Email(message = "email 형식이 올바르지 않습니다.")
            String email
    ) {}

    public record UpdatePhoneRequest(
            @NotBlank(message = "phone은 필수입니다.")
            String phone
    ) {}

    public record UpdateRoleRequest(
            @NotBlank(message = "role은 필수입니다.")
            String role
    ) {}

    public record UpdateActiveRegionRequest(String activeRegion) {}

    public record UpdateProfileImageRequest(String profileImageUrl) {}

    public record UpdateNickNameRequest(String nickName) {}

    public record UpdateCodeRequest(String code) {}

    public record UpdateFcmTokenRequest(String fcmToken) {}

    public record UpdatePrivacyRequest(
            @NotNull(message = "isPrivate는 필수입니다.")
            Boolean isPrivate
    ) {}
}
