package com.plateapp.plate_main.friend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendVisitCreateResponse {
    boolean ok;
    int count;
    List<Item> items;

    @Value
    @Builder
    public static class Item {
        Integer id;
        String username;
        String friendName;
        Integer storeId;
        String storeName;
        String address;
        String memo;
        LocalDate visitDate;
        LocalDateTime createdAt;
    }
}
