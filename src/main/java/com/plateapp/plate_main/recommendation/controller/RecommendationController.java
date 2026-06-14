package com.plateapp.plate_main.recommendation.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.recommendation.dto.RecommendationResponse;
import com.plateapp.plate_main.recommendation.service.RecommendationQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationQueryService recommendationQueryService;

    @GetMapping("/recommendations")
    public RecommendationResponse getRecommendations(
            @RequestParam(name = "surfaces", required = false) String surfaces,
            @RequestParam(name = "limitPerSurface", required = false) Integer limitPerSurface,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "currentMonth", required = false) Integer currentMonth,
            @RequestParam(name = "baseStoreId", required = false) Integer baseStoreId,
            @RequestParam(name = "baseFeedId", required = false) Integer baseFeedId,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "guestId", required = false) String guestId,
            @RequestParam(name = "isGuest", required = false) Boolean isGuest,
            Authentication authentication
    ) {
        return recommendationQueryService.getRecommendations(
                surfaces,
                limitPerSurface,
                lat,
                lng,
                currentMonth,
                baseStoreId,
                baseFeedId,
                username,
                isGuest,
                guestId,
                authentication
        );
    }
}
