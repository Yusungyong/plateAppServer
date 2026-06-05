package com.plateapp.plate_main.home.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.home.dto.HomeImpressionRequest;
import com.plateapp.plate_main.home.dto.HomeImpressionResponse;
import com.plateapp.plate_main.home.service.HomeImpressionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/home")
public class HomeImpressionController {

    private final HomeImpressionService impressionService;

    public HomeImpressionController(HomeImpressionService impressionService) {
        this.impressionService = impressionService;
    }

    @PostMapping("/impressions")
    public ApiResponse<HomeImpressionResponse> recordImpressions(
            @RequestBody @Valid HomeImpressionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(impressionService.record(request, authentication));
    }
}
