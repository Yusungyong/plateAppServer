package com.plateapp.plate_main.home.controller;

import com.plateapp.plate_main.home.dto.HomeRandomCandidatesResponse;
import com.plateapp.plate_main.home.service.HomeRandomCandidatesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home/random-candidates")
public class HomeRandomCandidatesController {

    private final HomeRandomCandidatesService homeRandomCandidatesService;

    public HomeRandomCandidatesController(HomeRandomCandidatesService homeRandomCandidatesService) {
        this.homeRandomCandidatesService = homeRandomCandidatesService;
    }

    @GetMapping("/recent")
    public HomeRandomCandidatesResponse getRecent(
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "include", required = false, defaultValue = "all") String include
    ) {
        return homeRandomCandidatesService.getRecent(limit, include);
    }

    @GetMapping("/nearby")
    public HomeRandomCandidatesResponse getNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radius", required = false) Double radius,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "include", required = false, defaultValue = "all") String include
    ) {
        return homeRandomCandidatesService.getNearby(lat, lng, radius, limit, include);
    }
}
