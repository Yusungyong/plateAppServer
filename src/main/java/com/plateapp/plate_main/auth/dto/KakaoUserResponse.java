// src/main/java/com/plateapp/plate_main/auth/dto/KakaoUserResponse.java
package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class KakaoUserResponse {

    private Long id;   // 카카오 고유 id

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {
        private String email;
        private KakaoProfile profile;
    }

    @Data
    public static class KakaoProfile {
        private String nickname;
    }
}
