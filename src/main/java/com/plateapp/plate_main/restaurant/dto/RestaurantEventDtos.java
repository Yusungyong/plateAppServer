package com.plateapp.plate_main.restaurant.dto;

import java.time.OffsetDateTime;

public final class RestaurantEventDtos {

    private RestaurantEventDtos() {
    }

    public record EventRecordRequest(
            String eventType,
            String eventUid,
            Boolean isGuest,
            String guestId,
            String sessionId,
            String deviceId,
            String surface,
            String source,
            Long menuId,
            String contentType,
            Long contentId,
            OffsetDateTime clientEventAt
    ) {
    }

    public record EventRecordResponse(
            Long eventId,
            boolean recorded,
            String eventType
    ) {
    }
}
