package com.plateapp.plate_main.map.dto;

public record MapSearchSuggestionDto(
        String placeId,
        String storeName,
        String address,
        Double lat,
        Double lng
) {}
