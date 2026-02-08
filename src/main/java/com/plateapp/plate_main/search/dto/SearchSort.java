package com.plateapp.plate_main.search.dto;

public enum SearchSort {
    RECENT,
    POPULAR,
    DISTANCE;

    public static SearchSort from(String value) {
        if (value == null || value.isBlank()) {
            return RECENT;
        }
        return SearchSort.valueOf(value.trim().toUpperCase());
    }
}
