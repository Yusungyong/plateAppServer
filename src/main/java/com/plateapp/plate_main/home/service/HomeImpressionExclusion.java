package com.plateapp.plate_main.home.service;

import java.util.Set;

public record HomeImpressionExclusion(
        Set<Integer> videoStoreIds,
        Set<Integer> imageFeedNos
) {
    public static HomeImpressionExclusion empty() {
        return new HomeImpressionExclusion(Set.of(), Set.of());
    }
}
