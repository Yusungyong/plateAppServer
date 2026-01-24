package com.plateapp.plate_main.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String bio;
    private String activeRegion;
    private String email;
    private String phoneNumber;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
