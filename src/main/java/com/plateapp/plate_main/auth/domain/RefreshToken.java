// src/main/java/com/plateapp/plate_main/auth/domain/RefreshToken.java
package com.plateapp.plate_main.auth.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "fp_103",
        indexes = {
                @Index(name = "idx_fp103_refresh_token", columnList = "refresh_token"),
                @Index(name = "idx_fp103_username_device", columnList = "username, device_id"),
                @Index(name = "idx_fp103_expiry", columnList = "expiry_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "expiry_date", nullable = false)
    private OffsetDateTime expiryDate;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
