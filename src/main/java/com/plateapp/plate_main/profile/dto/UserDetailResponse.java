package com.plateapp.plate_main.profile.dto;

import java.time.LocalDate;

public record UserDetailResponse(
    Integer userId,
    String username,
    String email,
    String phone,
    String role,
    LocalDate createdAt,
    LocalDate updatedAt,
    String activeRegion,
    String profileImageUrl,
    String nickName,
    String code,
    String fcmToken,
    Boolean isPrivate
) {}
