// src/main/java/com/plateapp/plate_main/auth/dto/GoogleIdTokenPayload.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleIdTokenPayload {

    private String iss;          // "accounts.google.com" 또는 "https://accounts.google.com"
    private String aud;          // 우리 클라이언트 ID
    private String sub;          // 구글 유저 고유 ID
    private String email;

    @JsonProperty("email_verified")
    private String emailVerified; // "true" / "false" 로 오는 경우 많음

    private Long exp;            // epoch seconds
    private Long iat;            // epoch seconds

    private String name;

    @JsonProperty("picture")
    private String pictureUrl;
}
