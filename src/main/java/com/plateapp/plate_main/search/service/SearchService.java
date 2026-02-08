package com.plateapp.plate_main.search.service;

import com.plateapp.plate_main.map.dto.MapSearchSuggestionDto;
import com.plateapp.plate_main.search.dto.SearchItem;
import com.plateapp.plate_main.search.dto.SearchResponse;
import com.plateapp.plate_main.search.dto.SearchScope;
import com.plateapp.plate_main.search.dto.SearchSuggestionItem;
import com.plateapp.plate_main.search.dto.SearchSuggestionResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.search.repository.SearchRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;

    public SearchSuggestionResponse suggest(String keyword, int limit, SearchScope scope) {
        if (keyword == null || keyword.isBlank()) {
            return new SearchSuggestionResponse(List.of());
        }
        int safeLimit = clamp(limit, 1, 20);
        SearchScope safeScope = (scope == null) ? SearchScope.ALL : scope;
        List<SearchSuggestionItem> items = new ArrayList<>();

        if (safeScope == SearchScope.PLACE || safeScope == SearchScope.ALL) {
            List<MapSearchSuggestionDto> places = searchRepository.suggestPlaces(keyword.trim(), safeLimit);
            for (MapSearchSuggestionDto place : places) {
                if (items.size() >= safeLimit) {
                    break;
                }
                items.add(new SearchSuggestionItem(
                    "place",
                    place.storeName(),
                    place.placeId(),
                    place.address(),
                    place.lat(),
                    place.lng(),
                    null
                ));
            }
        }

        if (items.size() < safeLimit && (safeScope == SearchScope.TAG || safeScope == SearchScope.ALL)) {
            int remaining = safeLimit - items.size();
            List<String> tags = searchRepository.suggestTags(keyword.trim(), remaining);
            for (String tag : tags) {
                items.add(new SearchSuggestionItem(
                    "tag",
                    "#" + tag,
                    null,
                    null,
                    null,
                    null,
                    tag
                ));
            }
        }

        return new SearchSuggestionResponse(items);
    }

    public SearchResponse search(SearchQuery query) {
        if (query.radiusM() != null && (query.lat() == null || query.lng() == null)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "lat/lng are required when radius is set");
        }
        if ((query.keyword() == null || query.keyword().isBlank())
            && (query.category() == null || query.category().isBlank())
            && (query.tags() == null || query.tags().isEmpty())
            && query.radiusM() == null) {
            return new SearchResponse(query.page(), query.size(), 0, List.of());
        }
        SearchPage page = searchRepository.search(query);
        List<SearchItem> items = page.items();
        return new SearchResponse(query.page(), query.size(), page.total(), items);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
