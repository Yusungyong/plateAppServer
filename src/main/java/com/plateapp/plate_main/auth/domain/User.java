// src/main/java/com/plateapp/plate_main/auth/domain/User.java
package com.plateapp.plate_main.auth.domain;

import java.time.LocalDate;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fp_100")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "username")   // PK
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role")
    private String role;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "nick_name")
    private String nickname;

    @Column(name = "active_region")
    private String activeRegion;

    @Column(name = "bio", length = 200)
    private String bio;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "code")
    private String code;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @PrePersist
    @PreUpdate
    private void normalizeAccountFields() {
        if (username != null) {
            username = username.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (nickname != null) {
            nickname = nickname.trim();
        }
    }
}
