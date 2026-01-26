package com.plateapp.plate_main.block.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BlockedUser {
    String blockedUsername;
    String blockedNickname;
    String blockedProfileImageUrl;
    LocalDateTime blockedAt;
    Integer blockedUserId;
    String blockedActiveRegion;
}
