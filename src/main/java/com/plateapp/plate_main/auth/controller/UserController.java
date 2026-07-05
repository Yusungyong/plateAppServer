// src/main/java/com/plateapp/plate_main/auth/controller/UserController.java
package com.plateapp.plate_main.auth.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.auth.domain.SocialAccount;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

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
                user.getRole(),
                social(user)
        ));
    }

    private SocialRes social(User user) {
        Integer userId = user.getUserId();
        if (userId == null) {
            return null;
        }

        return socialAccountRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toSocialRes)
                .orElse(null);
    }

    private SocialRes toSocialRes(SocialAccount account) {
        String provider = account.getProvider();
        return new SocialRes(provider == null ? null : provider.toLowerCase(Locale.ROOT));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MeRes(
            String username,
            String nickname,
            String email,
            String profileImageUrl,
            String role,
            SocialRes social
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SocialRes(
            String provider
    ) {}
}
