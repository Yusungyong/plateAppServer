package com.plateapp.plate_main.friend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

public class FriendVisitDtos {

    @Value
    @Builder
    public static class VisitItem {
        Integer id;
        String friendName;
        Integer storeId;
        String storeName;
        String address;
        String memo;
        LocalDate visitDate;
        String thumbnail;
        LocalDateTime createdAt;
    }

    @Value
    @Builder
    public static class VisitResponse {
        List<VisitItem> items;
        Integer nextCursor;
    }

    @Value
    @Builder
    public static class FriendInfo {
        String username;
        String nickname;
        String profileImageUrl;
    }

    @Value
    @Builder
    public static class FriendVisitResponse {
        FriendInfo friend;
        List<VisitItem> items;
        Integer nextCursor;
    }

    @Value
    @Builder
    public static class ScheduledVisitItem {
        Integer id;
        String friendName;
        Integer storeId;
        String storeName;
        String memo;
        LocalDate visitDate;
        String address;
        String thumbnail;
        LocalDateTime createdAt;
    }

    @Value
    @Builder
    public static class ScheduledVisitResponse {
        List<ScheduledVisitItem> items;
    }
}
