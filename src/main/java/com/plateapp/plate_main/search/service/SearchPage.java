package com.plateapp.plate_main.search.service;

import com.plateapp.plate_main.search.dto.SearchItem;
import java.util.List;

public record SearchPage(
        List<SearchItem> items,
        long total
) {}
