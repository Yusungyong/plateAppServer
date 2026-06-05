package com.plateapp.plate_main.home.controller;

import com.plateapp.plate_main.home.dto.HomeRandomCandidatesResponse;
import com.plateapp.plate_main.home.service.HomeRandomCandidatesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            @RequestParam(value = "include", required = false, defaultValue = "all") String include,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "isGuest", required = false, defaultValue = "false") boolean isGuest,
            @RequestParam(value = "guestId", required = false) String guestId
    ) {
        String resolvedUsername = isGuest ? null : resolveUsername(username);
        return homeRandomCandidatesService.getRecent(limit, include, resolvedUsername, isGuest, guestId);
    }

    @GetMapping("/nearby")
    public HomeRandomCandidatesResponse getNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radius", required = false) Double radius,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "include", required = false, defaultValue = "all") String include,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "isGuest", required = false, defaultValue = "false") boolean isGuest,
            @RequestParam(value = "guestId", required = false) String guestId
    ) {
        String resolvedUsername = isGuest ? null : resolveUsername(username);
        return homeRandomCandidatesService.getNearby(lat, lng, radius, limit, include, resolvedUsername, isGuest, guestId);
    }

    private String resolveUsername(String usernameParam) {
        if (usernameParam != null && !usernameParam.isBlank()) {
            return usernameParam;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
                return name;
            }
        }
        return null;
    }
}
