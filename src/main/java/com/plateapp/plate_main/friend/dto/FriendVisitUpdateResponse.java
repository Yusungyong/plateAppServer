package com.plateapp.plate_main.friend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendVisitUpdateResponse {
    boolean ok;
    Integer id;
    LocalDateTime updatedAt;
}
