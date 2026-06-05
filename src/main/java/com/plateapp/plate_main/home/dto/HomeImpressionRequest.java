package com.plateapp.plate_main.home.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record HomeImpressionRequest(
        String surface,
        String requestId,
        Boolean isGuest,
        String guestId,
        String sessionId,
        String deviceId,
        @NotEmpty
        @Size(max = 100)
        List<@Valid Item> items
) {
    public record Item(
            @NotBlank
            String contentType,
            Integer storeId,
            Integer feedNo,
            Integer positionNo,
            LocalDateTime clientImpressedAt
    ) {
    }
}
