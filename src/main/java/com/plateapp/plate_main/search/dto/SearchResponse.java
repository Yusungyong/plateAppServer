package com.plateapp.plate_main.search.dto;

import java.util.List;

public record SearchResponse(
        int page,
        int size,
        long total,
        List<SearchItem> items
) {}
