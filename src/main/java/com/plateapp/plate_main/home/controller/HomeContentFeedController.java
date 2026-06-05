package com.plateapp.plate_main.home.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.home.dto.HomeContentFeedResponse;
import com.plateapp.plate_main.home.service.HomeContentFeedService;

@RestController
@RequestMapping("/api/home")
public class HomeContentFeedController {

    private final HomeContentFeedService contentFeedService;

    public HomeContentFeedController(HomeContentFeedService contentFeedService) {
        this.contentFeedService = contentFeedService;
    }

    @GetMapping("/content-feed")
    public HomeContentFeedResponse getContentFeed(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "surface", defaultValue = "home-content") String surface,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "isGuest", defaultValue = "false") boolean isGuest,
            @RequestParam(name = "guestId", required = false) String guestId,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "radius", required = false) Double radius,
            Authentication authentication
    ) {
        String resolvedUsername = resolveUsername(username, isGuest, authentication);
        return contentFeedService.getContentFeed(
                cursor,
                limit,
                surface,
                resolvedUsername,
                resolvedUsername == null && isGuest,
                guestId,
                lat,
                lng,
                radius
        );
    }

    @GetMapping("/search/content")
    public HomeContentFeedResponse searchContentFeed(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "surface", defaultValue = "home-content-search") String surface,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "isGuest", defaultValue = "false") boolean isGuest,
            @RequestParam(name = "guestId", required = false) String guestId,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "radius", required = false) Double radius,
            Authentication authentication
    ) {
        String resolvedUsername = resolveUsername(username, isGuest, authentication);
        return contentFeedService.searchContentFeed(
                q,
                cursor,
                limit,
                surface,
                resolvedUsername,
                resolvedUsername == null && isGuest,
                guestId,
                lat,
                lng,
                radius
        );
    }

    private String resolveUsername(String usernameParam, boolean isGuest, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            String principal = String.valueOf(authentication.getPrincipal());
            if (principal != null && !principal.isBlank() && !"anonymousUser".equals(principal)) {
                return principal;
            }
        }
        if (isGuest) {
            return null;
        }
        return null;
    }
}
