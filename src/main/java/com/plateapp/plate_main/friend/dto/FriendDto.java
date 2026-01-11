package com.plateapp.plate_main.friend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendDto {
    Integer id;
    String username;
    String friendName;
    String friendNickname;
    String status;
    String friendProfileImageUrl;
    String friendActiveRegion;
    String initiatorUsername;
    String message;
    Long mutualCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime acceptedAt;
}
