package com.plateapp.plate_main.map.dto;

import java.util.List;

public record NearbyStoreMarkersResponse(
    List<NearbyStoreMarkerDto> items
) {}
