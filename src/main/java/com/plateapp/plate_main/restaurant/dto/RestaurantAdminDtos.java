package com.plateapp.plate_main.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class RestaurantAdminDtos {

    private RestaurantAdminDtos() {
    }

    public record RestaurantUpsertRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 300) String address,
            @Size(max = 40) String phone,
            @Size(max = 200) String businessHours,
            String introduction,
            String exposureStatus,
            @Size(min = 1, max = 4) List<@NotBlank @Size(max = 50) String> categories,
            @Valid List<RestaurantMediaRequest> media,
            @Valid List<RestaurantMenuRequest> menus
    ) {
    }

    public record RestaurantMenuRequest(
            @NotBlank @Size(max = 150) String name,
            BigDecimal price,
            String description,
            Integer displayOrder,
            @Valid List<RestaurantMediaRequest> media
    ) {
    }

    public record RestaurantMediaRequest(
            @NotBlank String mediaType,
            @NotBlank String usageType,
            @NotBlank String fileUrl,
            String originalName,
            String mimeType,
            Long fileSizeBytes,
            Integer displayOrder
    ) {
    }

    public record RestaurantIdResponse(Long restaurantId) {
    }

    public record RestaurantDeleteResponse(boolean deleted) {
    }

    public record RestaurantFileUploadResponse(
            String fileUrl,
            String originalName,
            String mimeType,
            Long fileSizeBytes
    ) {
    }

    public record RestaurantDetailResponse(
            Long id,
            String title,
            String address,
            String phone,
            String businessHours,
            String introduction,
            String exposureStatus,
            List<String> categories,
            List<RestaurantMediaResponse> media,
            List<RestaurantMenuResponse> menus,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record RestaurantMenuResponse(
            Long id,
            String name,
            BigDecimal price,
            String description,
            Integer displayOrder,
            List<RestaurantMediaResponse> media
    ) {
    }

    public record RestaurantMediaResponse(
            Long id,
            String mediaType,
            String usageType,
            String fileUrl,
            String originalName,
            String mimeType,
            Long fileSizeBytes,
            Integer displayOrder
    ) {
    }

    public record RestaurantListItem(
            Long id,
            String title,
            String address,
            List<String> categories,
            String exposureStatus,
            String representativeImageUrl,
            Long menuCount,
            OffsetDateTime updatedAt
    ) {
    }

    public record RestaurantListResponse(
            List<RestaurantListItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }
}
