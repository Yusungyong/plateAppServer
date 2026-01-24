package com.plateapp.plate_main.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeUserDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String activeRegion;
    private LocalDateTime likedAt;
}
