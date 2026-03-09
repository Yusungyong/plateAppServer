package com.plateapp.plate_main.search.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.search.dto.SearchResponse;
import com.plateapp.plate_main.search.dto.SearchScope;
import com.plateapp.plate_main.search.dto.SearchSort;
import com.plateapp.plate_main.search.dto.SearchSuggestionResponse;
import com.plateapp.plate_main.search.dto.SearchType;
import com.plateapp.plate_main.search.service.SearchQuery;
import com.plateapp.plate_main.search.service.SearchService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/suggest")
    public ResponseEntity<SearchSuggestionResponse> suggest(
        @RequestParam("q") String q,
        @RequestParam(value = "limit", defaultValue = "10") int limit,
        @RequestParam(value = "scope", required = false) String scope,
        @RequestParam(value = "isGuest", required = false) Boolean isGuest,
        @RequestParam(value = "guestId", required = false) String guestId
    ) {
        SearchScope safeScope = parseScope(scope);
        return ResponseEntity.ok(searchService.suggest(q, limit, safeScope));
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "tags", required = false) String tags,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "lat", required = false) Double lat,
        @RequestParam(value = "lng", required = false) Double lng,
        @RequestParam(value = "sort", required = false) String sort,
        @RequestParam(value = "isGuest", required = false) Boolean isGuest,
        @RequestParam(value = "guestId", required = false) String guestId
    ) {
        SearchType safeType = parseType(type);
        SearchSort safeSort = parseSort(sort);
        int safePage = Math.max(0, page);
        int safeSize = clamp(size, 1, 50);
        List<String> tagList = parseTags(tags);

        SearchQuery query = new SearchQuery(
            q,
            safeType,
            category,
            tagList,
            radius,
            lat,
            lng,
            safeSort,
            safePage,
            safeSize
        );

        return ResponseEntity.ok(searchService.search(query));
    }

    private SearchScope parseScope(String value) {
        try {
            return SearchScope.from(value);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid scope");
        }
    }

    private SearchType parseType(String value) {
        try {
            return SearchType.from(value);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid type");
        }
    }

    private SearchSort parseSort(String value) {
        try {
            return SearchSort.from(value);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid sort");
        }
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .map(tag -> tag.toLowerCase(Locale.US))
            .distinct()
            .toList();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
