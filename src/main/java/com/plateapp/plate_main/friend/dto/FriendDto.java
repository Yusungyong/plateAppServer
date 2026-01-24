package com.plateapp.plate_main.friend.dto;

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
public class FriendDTO {
    // For friend management API
    private Integer userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String activeRegion;
    private LocalDateTime friendsSince;

    // For existing friend features
    private Integer id;
    private String friendName;
    private String friendNickname;
    private String status;
    private String friendProfileImageUrl;
    private String friendActiveRegion;
    private String initiatorUsername;
    private String message;
    private Long mutualCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acceptedAt;
}
