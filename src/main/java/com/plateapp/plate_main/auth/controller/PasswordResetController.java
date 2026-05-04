package com.plateapp.plate_main.auth.controller;

import com.plateapp.plate_main.auth.service.PasswordResetService;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.resetPassword(
                    request.email(),
                    request.verificationCode(),
                    request.newPassword()
            );
            return ApiResponse.ok(null, "Password has been changed successfully.");
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "An error occurred while resetting the password.");
        }
    }

    public record PasswordResetRequest(
            @NotBlank(message = "email is required")
            @Email(message = "email format is invalid")
            String email,

            @NotBlank(message = "verificationCode is required")
            String verificationCode,

            @NotBlank(message = "newPassword is required")
            String newPassword
    ) {}
}
