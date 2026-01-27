package com.plateapp.plate_main.map.dto;

public record NearbyStoreMarkerDto(
    Integer storeId,
    String placeId,
    String storeName,
    String address,
    String thumbnail,
    String groupId,
    Double lat,
    Double lng,
    Integer distanceM,
    Integer feedCount,
    String contentType,
    Integer representativeFeedId
) {}
