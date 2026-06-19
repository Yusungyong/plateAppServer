package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.auth.service.AccountValidationService;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.security.RateLimitService;
import com.plateapp.plate_main.owner.dto.OwnerAccountValidationDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/signup-account-validations")
@RequiredArgsConstructor
public class OwnerAccountValidationController {

    private final AccountValidationService accountValidationService;
    private final RateLimitService rateLimitService;

    @PostMapping
    public ApiResponse<OwnerAccountValidationDtos.Response> validate(
            @Valid @RequestBody OwnerAccountValidationDtos.Request request,
            HttpServletRequest httpRequest
    ) {
        rateLimitService.check(
                "owner:signup-account-validation:" + rateLimitService.clientIp(httpRequest),
                60,
                Duration.ofMinutes(1)
        );

        AccountValidationService.ValidationResult result =
                accountValidationService.validateAvailability(request.field(), request.value());
        return ApiResponse.ok(new OwnerAccountValidationDtos.Response(
                result.field(),
                result.value(),
                result.available(),
                result.message()
        ));
    }
}
