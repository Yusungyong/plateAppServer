package com.plateapp.plate_main.auth.dto;

import com.plateapp.plate_main.auth.domain.User;

public record AuthUserDto(
        String username,
        String email,
        String nickname,
        String profileImageUrl,
        String activeRegion
) {
    public static AuthUserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new AuthUserDto(
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getActiveRegion()
        );
    }
}
