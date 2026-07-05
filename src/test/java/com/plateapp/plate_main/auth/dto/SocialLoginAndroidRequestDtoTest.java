package com.plateapp.plate_main.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SocialLoginAndroidRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void kakaoAndroidPayloadDeserializes() throws Exception {
        KakaoLoginRequest request = objectMapper.readValue("""
                {
                  "accessToken": "kakao-access-token",
                  "deviceId": "android-device-id",
                  "deviceModel": "Pixel 8",
                  "os": "Android",
                  "osVersion": "16",
                  "appVersion": "2.0.5",
                  "fcmToken": "fcm-token",
                  "ignoredFutureField": "ignored"
                }
                """, KakaoLoginRequest.class);

        assertThat(request.getAccessToken()).isEqualTo("kakao-access-token");
        assertThat(request.getDeviceId()).isEqualTo("android-device-id");
        assertThat(request.getDeviceModel()).isEqualTo("Pixel 8");
        assertThat(request.getOs()).isEqualTo("Android");
        assertThat(request.getOsVersion()).isEqualTo("16");
        assertThat(request.getAppVersion()).isEqualTo("2.0.5");
        assertThat(request.getFcmToken()).isEqualTo("fcm-token");
    }

    @Test
    void googleAndroidPayloadDeserializes() throws Exception {
        GoogleLoginRequest request = objectMapper.readValue("""
                {
                  "idToken": "google-id-token",
                  "deviceId": "android-device-id",
                  "deviceModel": "Pixel 8",
                  "os": "Android",
                  "osVersion": "16",
                  "appVersion": "2.0.5",
                  "fcmToken": "fcm-token",
                  "ignoredFutureField": "ignored"
                }
                """, GoogleLoginRequest.class);

        assertThat(request.getIdToken()).isEqualTo("google-id-token");
        assertThat(request.getDeviceId()).isEqualTo("android-device-id");
        assertThat(request.getDeviceModel()).isEqualTo("Pixel 8");
        assertThat(request.getOs()).isEqualTo("Android");
        assertThat(request.getOsVersion()).isEqualTo("16");
        assertThat(request.getAppVersion()).isEqualTo("2.0.5");
        assertThat(request.getFcmToken()).isEqualTo("fcm-token");
    }
}
