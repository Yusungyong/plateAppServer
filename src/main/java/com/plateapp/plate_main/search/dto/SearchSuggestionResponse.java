package com.plateapp.plate_main.search.dto;

import java.util.List;

public record SearchSuggestionResponse(
        List<SearchSuggestionItem> items
) {}
