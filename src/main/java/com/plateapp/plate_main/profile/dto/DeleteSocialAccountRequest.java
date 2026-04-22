package com.plateapp.plate_main.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeleteSocialAccountRequest(
        String provider,
        String identityToken,
        String authorizationCode,
        String idToken,
        String accessToken,
        String reason
) {
}
