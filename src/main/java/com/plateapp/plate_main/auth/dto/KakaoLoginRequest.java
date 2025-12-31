// src/main/java/com/plateapp/plate_main/auth/dto/KakaoLoginRequest.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoLoginRequest {

    /** 카카오 SDK에서 받은 access_token */
    private String accessToken;
}
