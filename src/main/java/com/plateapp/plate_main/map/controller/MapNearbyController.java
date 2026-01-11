package com.plateapp.plate_main.map.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.map.dto.NearbyStoreMarkersResponse;
import com.plateapp.plate_main.service.MapNearbyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class MapNearbyController {

  private final MapNearbyService mapNearbyService;

  @GetMapping("/stores/nearby")
  public ResponseEntity<NearbyStoreMarkersResponse> getNearbyStores(
      @RequestParam("lat") double lat,
      @RequestParam("lng") double lng,
      @RequestParam(value = "radius", defaultValue = "1500") int radius,
      @RequestParam(value = "limit", defaultValue = "60") int limit
  ) {
    return ResponseEntity.ok(mapNearbyService.findNearby(lat, lng, radius, limit));
  }

  @GetMapping("/stores/search")
  public ResponseEntity<NearbyStoreMarkersResponse> searchStores(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "lat", required = false) Double lat,
      @RequestParam(value = "lng", required = false) Double lng,
      @RequestParam(value = "limit", defaultValue = "20") int limit
  ) {
    return ResponseEntity.ok(mapNearbyService.search(keyword, lat, lng, limit));
  }

  @GetMapping("/stores/suggest")
  public ResponseEntity<java.util.List<com.plateapp.plate_main.map.dto.MapSearchSuggestionDto>> suggestStores(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "limit", defaultValue = "10") int limit
  ) {
    return ResponseEntity.ok(mapNearbyService.suggest(keyword, limit));
  }
}
