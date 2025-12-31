// src/main/java/com/plateapp/plate_main/auth/controller/PasswordResetController.java
package com.plateapp.plate_main.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.auth.service.PasswordResetService;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.resetPassword(request.email(), request.newPassword());
            return ApiResponse.ok(null, "비밀번호가 성공적으로 변경되었습니다.");
        } catch (AppException e) {
            // 서비스가 AppException으로 표준 던지면 그대로 전파 (GlobalExceptionHandler가 처리)
            throw e;
        } catch (Exception e) {
            // 서비스가 아직 표준 예외를 안 던지는 경우를 대비한 안전망
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "비밀번호 변경 중 오류가 발생했습니다.");
        }
    }

    public record PasswordResetRequest(
            @NotBlank(message = "email은 필수입니다.")
            @Email(message = "email 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "newPassword는 필수입니다.")
            String newPassword
    ) {}
}
