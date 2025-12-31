// src/main/java/com/plateapp/plate_main/auth/domain/SocialAccount.java
package com.plateapp.plate_main.auth.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "fp_110",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fp110_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    private int socialId;

    /** fp_100.user_id (내부 회원 고유 ID) */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /** APPLE / GOOGLE / KAKAO / NAVER 등 */
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    /** provider가 발급한 고유 식별자 (sub / id 등) */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    /** 소셜에서 전달된 이메일 (nullable) */
    @Column(name = "email", length = 255)
    private String email;

    /** 소셜 계정 이름 / 닉네임 (nullable) */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /** 등록 시각 (DB default now() 사용 가능) */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
