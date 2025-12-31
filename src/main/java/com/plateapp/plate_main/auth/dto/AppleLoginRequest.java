// src/main/java/com/plateapp/plate_main/auth/dto/AppleLoginRequest.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 클라이언트에서 전달하는 애플 로그인 요청 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // deviceInfo 등 추가 필드는 무시
public class AppleLoginRequest {

    /** @invertase/react-native-apple-authentication 에서 내려주는 identityToken (JWT) */
    private String identityToken;

    /** 필요시 애플 서버와 통신할 때 사용할 수 있는 authorizationCode */
    private String authorizationCode;

    /** response.user 값 (애플 사용자 ID, sub 와 동일하거나 유사) */
    private String user;
}
