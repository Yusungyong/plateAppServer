package com.plateapp.plate_main.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchItem(
        String type,
        String placeId,
        Integer storeId,
        String storeName,
        String address,
        Double lat,
        Double lng,
        Integer distanceM,
        Integer feedCount,
        String contentType,
        Integer imageFeedId,
        String thumbnail,
        String title,
        Integer feedId,
        OffsetDateTime createdAt
) {}
