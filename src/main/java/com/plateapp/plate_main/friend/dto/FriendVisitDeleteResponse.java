package com.plateapp.plate_main.friend.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendVisitDeleteResponse {
    boolean ok;
    Integer id;
}
