package com.plateapp.plate_main.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "password is required")
        String password,
        String reason
) {
}
