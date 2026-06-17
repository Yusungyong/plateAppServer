package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.security.RateLimitService;
import com.plateapp.plate_main.owner.dto.BusinessVerificationDtos;
import com.plateapp.plate_main.owner.service.NtsBusinessVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/business-verifications")
@RequiredArgsConstructor
public class BusinessVerificationController {

    private final NtsBusinessVerificationService verificationService;
    private final RateLimitService rateLimitService;

    @PostMapping
    public ApiResponse<BusinessVerificationDtos.VerifyResponse> verify(
            @Valid @RequestBody BusinessVerificationDtos.VerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        rateLimitService.check(
                "owner:business-verification:"
                        + rateLimitService.clientIp(httpRequest)
                        + ":"
                        + rateLimitService.identity(request.businessNumber()),
                10,
                Duration.ofMinutes(10)
        );
        return ApiResponse.ok(verificationService.verify(request));
    }
}
