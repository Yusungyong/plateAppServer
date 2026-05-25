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
@Table(name = "fp_111")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialSignupSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "signup_token", nullable = false, length = 255, unique = true)
    private String signupToken;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "nickname", length = 255)
    private String nickname;

    @Column(name = "raw_profile_json", columnDefinition = "text")
    private String rawProfileJson;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
