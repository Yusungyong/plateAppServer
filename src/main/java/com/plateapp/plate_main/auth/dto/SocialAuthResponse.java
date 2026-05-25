package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.plateapp.plate_main.auth.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocialAuthResponse {

    private String kind;
    private String accessToken;
    private String refreshToken;
    private AuthUserDto user;
    private String signupToken;
    private String provider;
    private String providerUserId;
    private String email;
    private String nickname;

    public static SocialAuthResponse loginSuccess(String accessToken, String refreshToken, User user) {
        return SocialAuthResponse.builder()
                .kind("login_success")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthUserDto.from(user))
                .build();
    }

    public static SocialAuthResponse signupRequired(
            String signupToken,
            String provider,
            String providerUserId,
            String email,
            String nickname
    ) {
        return SocialAuthResponse.builder()
                .kind("signup_required")
                .signupToken(signupToken)
                .provider(provider == null ? null : provider.toLowerCase())
                .providerUserId(providerUserId)
                .email(email)
                .nickname(nickname)
                .build();
    }
}
