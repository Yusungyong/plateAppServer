package com.plateapp.plate_main.friend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendSearchDto {
    String username;
    String nickname;
    String profileImageUrl;
    String activeRegion;
    Long mutualCount;
}
