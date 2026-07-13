package com.plateapp.plate_main.restaurant.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.restaurant.dto.RestaurantEventDtos;
import com.plateapp.plate_main.restaurant.service.RestaurantEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/events")
@RequiredArgsConstructor
public class RestaurantEventController {

    private final RestaurantEventService restaurantEventService;

    @PostMapping
    public ApiResponse<RestaurantEventDtos.EventRecordResponse> record(
            @PathVariable Long restaurantId,
            @RequestBody RestaurantEventDtos.EventRecordRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(restaurantEventService.record(
                restaurantId,
                request,
                username(authentication),
                servletRequest.getHeader(HttpHeaders.USER_AGENT)
        ));
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return null;
        }
        return name;
    }
}
