package com.plateapp.plate_main.search.dto;

public enum SearchScope {
    PLACE,
    TAG,
    ALL;

    public static SearchScope from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return SearchScope.valueOf(value.trim().toUpperCase());
    }
}
