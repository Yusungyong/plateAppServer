package com.plateapp.plate_main.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialSignupCompleteRequest(
        @NotBlank String signupToken,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 50) String nickname,
        @NotNull Boolean agreeService,
        @NotNull Boolean agreePrivacy
) {}
