// src/main/java/com/plateapp/plate_main/auth/dto/GoogleLoginRequest.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleLoginRequest {

    /** 구글 로그인 SDK에서 받은 idToken */
    private String idToken;
}
