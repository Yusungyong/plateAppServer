package com.plateapp.plate_main.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchSuggestionItem(
        String type,
        String label,
        String placeId,
        String address,
        Double lat,
        Double lng,
        String tag
) {}
