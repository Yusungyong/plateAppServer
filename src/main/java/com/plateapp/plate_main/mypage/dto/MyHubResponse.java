package com.plateapp.plate_main.mypage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record MyHubResponse(
        Profile profile,
        Counts counts,
        List<Section> availableSections,
        List<ContentPreview> recentContent,
        List<ContentPreview> recentLikes,
        PrimaryAction primaryAction,
        Instant generatedAt
) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Profile(
            String username,
            String displayName,
            String profileImageUrl,
            String activeRegion,
            boolean isPrivate
    ) {
    }

    public record Counts(
            long contentCount,
            long videoCount,
            long imageCount,
            long likedContentCount,
            long receivedLikeCount,
            long friendCount,
            long pendingFriendRequestCount
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ContentPreview(
            ContentType contentType,
            String contentId,
            Integer videoStoreId,
            Integer imageFeedId,
            String placeId,
            String title,
            String thumbnailUrl,
            Store store,
            Author author,
            Instant createdAt,
            LocalDate createdOn,
            TimePrecision createdTimePrecision,
            Instant likedAt,
            LocalDate likedOn,
            TimePrecision likedTimePrecision
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Store(
            String placeId,
            String storeName,
            String address,
            Double latitude,
            Double longitude
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Author(
            String username,
            String displayName,
            String profileImageUrl
    ) {
    }

    public enum Section {
        RECENT_CONTENT,
        LIKED_CONTENT
    }

    public enum PrimaryAction {
        EXPLORE_CONTENT,
        SET_ACTIVE_REGION,
        CREATE_CONTENT,
        FIND_FRIENDS
    }

    public enum ContentType {
        VIDEO,
        IMAGE
    }

    public enum TimePrecision {
        EXACT,
        DATE
    }
}
