package com.plateapp.plate_main.like.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LikeUserResponse {
    Integer userId;
    String username;
    String nickname;
    String profileImageUrl;
    String activeRegion;
    LocalDateTime likedAt;
}
