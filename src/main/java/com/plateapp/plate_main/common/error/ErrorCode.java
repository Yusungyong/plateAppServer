// src/main/java/com/plateapp/plate_main/common/error/ErrorCode.java
package com.plateapp.plate_main.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // COMMON (공통)
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 값이 올바르지 않습니다."),
    COMMON_MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_401", "필수 파라미터가 누락되었습니다."),
    COMMON_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "COMMON_402", "파라미터 타입이 올바르지 않습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "리소스를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "허용되지 않은 메서드입니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "COMMON_409", "요청이 충돌했습니다."),
    COMMON_JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "COMMON_410", "요청 JSON 파싱에 실패했습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다."),

    // AUTH (인증/인가)
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "권한이 없습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_402", "토큰이 만료되었습니다."),
    AUTH_SOCIAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_SOCIAL_TOKEN_INVALID", "소셜 로그인 토큰을 검증하지 못했습니다."),
    AUTH_SOCIAL_AUDIENCE_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_SOCIAL_AUDIENCE_MISMATCH", "허용되지 않은 소셜 로그인 클라이언트입니다."),
    AUTH_SOCIAL_PROVIDER_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_SOCIAL_PROVIDER_USER_NOT_FOUND", "소셜 로그인 사용자 정보를 찾을 수 없습니다."),
    AUTH_SOCIAL_ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "AUTH_SOCIAL_ACCOUNT_BLOCKED", "사용할 수 없는 소셜 계정입니다."),

    // ✅ refresh 전용 (추가)
    AUTH_REFRESH_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_411", "리프레시 토큰이 만료되었습니다."),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_412", "리프레시 토큰이 올바르지 않습니다."),

    // USER (예시)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),

    // SEASON FOOD
    SEASON_FOOD_INVALID_MONTH(HttpStatus.BAD_REQUEST, "SEASON_FOOD_INVALID_MONTH", "month must be between 1 and 12."),
    SEASON_FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "SEASON_FOOD_NOT_FOUND", "Season food ingredient not found."),
    SEASON_REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "SEASON_REGION_NOT_FOUND", "Season region not found."),
    SEASON_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SEASON_CATEGORY_NOT_FOUND", "Season food category not found."),

    // VIDEO/COMMENT 등
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "VIDEO_404", "동영상을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "댓글을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
