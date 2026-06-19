package com.plateapp.plate_main.owner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.owner.dto.BusinessVerificationDtos;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class NtsBusinessVerificationService {

    private static final String PROVIDER = "NTS";
    private static final DateTimeFormatter NTS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestTemplateBuilder restTemplateBuilder;
    private final String baseUrl;
    private final String serviceKey;
    private final long timeoutMs;
    private final boolean enabled;

    public NtsBusinessVerificationService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${external.nts-business.base-url:https://api.odcloud.kr/api/nts-businessman/v1}") String baseUrl,
            @Value("${external.nts-business.service-key:}") String serviceKey,
            @Value("${external.nts-business.timeout-ms:5000}") long timeoutMs,
            @Value("${external.nts-business.enabled:true}") boolean enabled
    ) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.timeoutMs = timeoutMs;
        this.enabled = enabled;
    }

    public BusinessVerificationDtos.VerifyResponse verify(BusinessVerificationDtos.VerifyRequest request) {
        String businessNumber = normalizeBusinessNumber(request.businessNumber());
        if (!enabled || serviceKey.isBlank()) {
            throw unavailable();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("businesses", List.of(Map.of(
                "b_no", businessNumber,
                "start_dt", request.openingDate().format(NTS_DATE),
                "p_nm", request.representativeName().trim(),
                "b_nm", nullToBlank(request.businessName())
        )));

        try {
            RestTemplate restTemplate = restTemplateBuilder
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .readTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            JsonNode response = restTemplate.postForObject(
                    baseUrl + "/validate?serviceKey=" + serviceKey,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            return toResponse(response);
        } catch (RestClientException | IllegalArgumentException e) {
            throw unavailable();
        }
    }

    private BusinessVerificationDtos.VerifyResponse toResponse(JsonNode response) {
        JsonNode first = response == null ? null : response.path("data").path(0);
        if (first == null || first.isMissingNode()) {
            throw unavailable();
        }

        String valid = first.path("valid").asText("");
        boolean verified = "01".equals(valid);
        String status = verified ? "verified" : "rejected";
        String message = verified
                ? "사업자 정보가 확인되었습니다."
                : "사업자등록번호, 대표자명 또는 개업일자가 일치하지 않습니다.";
        return new BusinessVerificationDtos.VerifyResponse(
                verified,
                status,
                message,
                PROVIDER,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private String normalizeBusinessNumber(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (normalized.length() != 10) {
            throw new AppException(ErrorCode.BUSINESS_NUMBER_INVALID);
        }
        return normalized;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private AppException unavailable() {
        return new AppException(ErrorCode.BUSINESS_VERIFICATION_UNAVAILABLE);
    }
}
