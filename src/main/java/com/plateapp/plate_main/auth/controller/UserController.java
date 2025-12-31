// src/main/java/com/plateapp/plate_main/auth/controller/UserController.java
package com.plateapp.plate_main.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<MeRes> me(Authentication authentication) {
        log.debug("/me 호출");

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }

        String username = String.valueOf(authentication.getPrincipal());

        User user = userRepository.findById(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        return ApiResponse.ok(new MeRes(
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole()
        ));
    }

    public record MeRes(
            String username,
            String nickname,
            String email,
            String profileImageUrl,
            String role
    ) {}
}
