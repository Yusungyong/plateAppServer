package com.plateapp.plate_main.search.service;

import com.plateapp.plate_main.search.dto.SearchSort;
import com.plateapp.plate_main.search.dto.SearchType;
import java.util.List;

public record SearchQuery(
        String keyword,
        SearchType type,
        String category,
        List<String> tags,
        Integer radiusM,
        Double lat,
        Double lng,
        SearchSort sort,
        int page,
        int size
) {}
