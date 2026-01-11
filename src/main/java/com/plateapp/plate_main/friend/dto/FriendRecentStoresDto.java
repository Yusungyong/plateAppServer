package com.plateapp.plate_main.friend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

public class FriendRecentStoresDto {

    @Value
    @Builder
    public static class FriendVisitSummary {
        String friendName;
        LocalDate visitDate;
    }

    @Value
    @Builder
    public static class RecentStoreItem {
        Integer storeId;
        String storeName;
        String address;
        String placeId;
        Long visitCount;
        LocalDateTime lastVisitedAt;
        List<FriendVisitSummary> friends;
        String thumbnail;
    }

    @Value
    @Builder
    public static class RecentStoreResponse {
        List<RecentStoreItem> items;
    }
}
