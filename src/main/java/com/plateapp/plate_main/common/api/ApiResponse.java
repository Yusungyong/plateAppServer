// src/main/java/com/plateapp/plate_main/common/api/ApiResponse.java
package com.plateapp.plate_main.common.api;

import java.time.Instant;

import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.plateapp.plate_main.common.filter.RequestIdFilter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;
    private final String requestId;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, String message, String errorCode, String requestId, Instant timestamp) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    private static String currentRequestId() {
        String rid = MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID);
        return (rid == null || rid.isBlank()) ? null : rid;
    }

    // ===== OK =====
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, currentRequestId(), Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, currentRequestId(), Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null, currentRequestId(), Instant.now());
    }

    // ===== FAIL =====
    public static ApiResponse<Void> fail(String errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode, currentRequestId(), Instant.now());
    }

    public static <T> ApiResponse<T> fail(String errorCode, String message, T data) {
        return new ApiResponse<>(false, data, message, errorCode, currentRequestId(), Instant.now());
    }

    // getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public String getRequestId() { return requestId; }
    public Instant getTimestamp() { return timestamp; }
}
