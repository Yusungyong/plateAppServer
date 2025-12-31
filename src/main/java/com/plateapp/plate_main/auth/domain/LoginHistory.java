// src/main/java/com/plateapp/plate_main/auth/domain/LoginHistory.java
package com.plateapp.plate_main.auth.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fp_105")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_id")
    private Long loginId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "login_datetime")
    private OffsetDateTime loginDatetime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "login_status")
    private String loginStatus; // SUCCESS / FAIL

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "os")
    private String os;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
