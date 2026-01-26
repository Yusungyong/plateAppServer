package com.plateapp.plate_main.service;

import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.map.dto.NearbyStoreMarkerDto;
import com.plateapp.plate_main.map.dto.NearbyStoreMarkersResponse;
import com.plateapp.plate_main.map.repository.MapNearbyRepository;
import com.plateapp.plate_main.report.repository.ReportRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapNearbyService {

    private final MapNearbyRepository mapNearbyRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;

    public NearbyStoreMarkersResponse findNearby(double lat, double lng, int radiusM, int limit, String username) {
        int safeRadius = clamp(radiusM, 100, 50_000); // 100m ~ 50km
        int safeLimit = clamp(limit, 1, 200);         // avoid flooding map with markers

        List<String> excluded = List.copyOf(loadExcludedUsernames(username));
        List<NearbyStoreMarkerDto> items = mapNearbyRepository.findNearby(lat, lng, safeRadius, safeLimit, excluded);
        return new NearbyStoreMarkersResponse(items);
    }

    public NearbyStoreMarkersResponse search(String keyword, Double lat, Double lng, int limit, String username) {
        if (keyword == null || keyword.isBlank()) {
            return new NearbyStoreMarkersResponse(List.of());
        }
        int safeLimit = clamp(limit, 1, 50); // cap search results
        List<String> excluded = List.copyOf(loadExcludedUsernames(username));
        List<NearbyStoreMarkerDto> items = mapNearbyRepository.searchStores(keyword.trim(), lat, lng, safeLimit, excluded);
        return new NearbyStoreMarkersResponse(items);
    }

    public List<com.plateapp.plate_main.map.dto.MapSearchSuggestionDto> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int safeLimit = clamp(limit, 1, 15); // shorter list for autocomplete
        return mapNearbyRepository.suggestStores(keyword.trim(), safeLimit);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private Set<String> loadExcludedUsernames(String username) {
        if (username == null || username.isBlank()) {
            return Set.of();
        }
        Set<String> excluded = new HashSet<>();
        List<String> blocked = blockRepository.findBlockedUsernames(username);
        if (blocked != null) {
            excluded.addAll(blocked);
        }
        List<String> reported = reportRepository.findReportedUsernames(username);
        if (reported != null) {
            excluded.addAll(reported);
        }
        return excluded;
    }
}
