package com.plateapp.plate_main.friend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendStoreVisitDto {

    Integer storeId;
    String storeName;
    List<FriendVisit> friends;

    @Value
    @Builder
    public static class FriendVisit {
        String friendName;
        String nickname;
        String profileImageUrl;
        String memo;
        LocalDate visitDate;
        LocalDateTime createdAt;
    }
}
