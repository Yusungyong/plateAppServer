// src/main/java/com/plateapp/plate_main/auth/domain/User.java
package com.plateapp.plate_main.auth.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "code")
    private String code;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "is_private")
    private Boolean isPrivate;
}
