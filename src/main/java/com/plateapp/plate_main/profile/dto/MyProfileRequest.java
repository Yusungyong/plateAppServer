package com.plateapp.plate_main.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record MyProfileRequest(
    @NotBlank(message = "username은 필수입니다.")
    String username,
    Boolean includeStats
) {}
