package com.plateapp.plate_main.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleLoginRequest {

    private String idToken;

    private String deviceId;

    private String deviceModel;

    private String os;

    private String osVersion;

    private String appVersion;

    private String fcmToken;
}
