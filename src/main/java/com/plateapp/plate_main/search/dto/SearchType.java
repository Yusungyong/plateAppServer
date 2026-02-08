package com.plateapp.plate_main.search.dto;

public enum SearchType {
    ALL,
    PLACE,
    VIDEO,
    IMAGE;

    public static SearchType from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return SearchType.valueOf(value.trim().toUpperCase());
    }
}
