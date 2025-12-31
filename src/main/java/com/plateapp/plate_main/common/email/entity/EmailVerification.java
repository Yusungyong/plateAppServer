// src/main/java/com/plateapp/platemain/common/email/entity/EmailVerification.java
package com.plateapp.plate_main.common.email.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fp_120")
@Getter
@Setter
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // fp_120 에 PK 없으면 새로 추가하는 걸 추천

    @Column(nullable = false)
    private String email;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
