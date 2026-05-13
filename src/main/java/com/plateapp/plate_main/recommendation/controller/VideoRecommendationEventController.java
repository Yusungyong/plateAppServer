package com.plateapp.plate_main.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.recommendation.dto.VideoRecommendationEventRequest;
import com.plateapp.plate_main.recommendation.dto.VideoRecommendationEventResponse;
import com.plateapp.plate_main.recommendation.service.VideoRecommendationEventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class VideoRecommendationEventController {

    private final VideoRecommendationEventService eventService;

    @PostMapping("/video-events")
    public ResponseEntity<VideoRecommendationEventResponse> recordVideoEvent(
            @RequestBody @Valid VideoRecommendationEventRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(eventService.record(request, authentication, servletRequest));
    }
}
