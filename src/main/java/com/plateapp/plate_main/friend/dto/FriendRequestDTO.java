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
public class FriendRequestDTO {
    private Long requestId;
    private Integer fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromProfileImageUrl;
    private Integer toUserId;
    private String toUsername;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
