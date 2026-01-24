package com.plateapp.plate_main.profile.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileDetailResponse {
    String username;
    String nickName;
    String profileImageUrl;
    String activeRegion;
    Stats stats;
    Friends friends;
    Account account;
    Social social;
    Login login;
    Preferences preferences;
    Safety safety;

    @Value
    @Builder
    public static class Stats {
        long videoCount;
        long imageCount;
        long likeCount;
        LocalDateTime recentActivityAt;
    }

    @Value
    @Builder
    public static class Friends {
        long count;
    }

    @Value
    @Builder
    public static class Account {
        String email;
        String phone;
        String role;
        LocalDate createdAt;
    }

    @Value
    @Builder
    public static class Social {
        String provider;
        String providerUserId;
        String displayName;
    }

    @Value
    @Builder
    public static class Login {
        LocalDateTime lastLoginAt;
        String lastLoginIp;
        String lastLoginStatus;
        String lastFailReason;
    }

    @Value
    @Builder
    public static class Preferences {
        Boolean notifications;
        Filters filters;
    }

    @Value
    @Builder
    public static class Filters {
        String filterType;
        Boolean imageYn;
        String timeFilter;
        String regionFilter;
        String postSorted;
    }

    @Value
    @Builder
    public static class Safety {
        long blockedCount;
        long reportCount;
    }
}
