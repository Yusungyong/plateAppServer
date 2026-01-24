package com.plateapp.plate_main.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendSearchResultDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String activeRegion;
    private boolean isFriend;
    private boolean isPending;
}
