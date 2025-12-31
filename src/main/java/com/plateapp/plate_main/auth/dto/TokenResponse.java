package com.plateapp.plate_main.auth.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Object user; // 또는 User 타입
}