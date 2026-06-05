package com.plateapp.plate_main.profile.dto;

public record MyProfileRequest(
    String username,
    Boolean includeStats
) {}
