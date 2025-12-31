// src/main/java/com/plateapp/plate_main/auth/dto/AppleIdTokenPayload.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Apple identityToken(JWT) payload 디코딩용 DTO
 * 실제 JWT 내용의 일부만 사용.
 */
@Data
public class AppleIdTokenPayload {

    private String iss;   // 보통 "https://appleid.apple.com"
    private String aud;   // 우리 서비스의 client-id (bundle id 또는 services id)
    private String sub;   // 애플 유저 고유 ID
    private String email;

    @JsonProperty("email_verified")
    private String emailVerified; // "true" 또는 "false"

    private Long iat; // 발급 시간 (epoch seconds)
    private Long exp; // 만료 시간 (epoch seconds)
}
