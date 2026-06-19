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
    ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", "이미 사용 중인 계정 정보가 있습니다."),

    // ✅ refresh 전용 (추가)
    AUTH_REFRESH_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_411", "리프레시 토큰이 만료되었습니다."),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_412", "리프레시 토큰이 올바르지 않습니다."),

    // USER (예시)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),

    // VIDEO/COMMENT 등
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "VIDEO_404", "동영상을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "댓글을 찾을 수 없습니다."),

    STORE_APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_APPROVAL_NOT_FOUND", "매장 신청 정보를 찾을 수 없습니다."),
    STORE_APPROVAL_VERSION_CONFLICT(HttpStatus.CONFLICT, "STORE_APPROVAL_VERSION_CONFLICT", "다른 운영자가 먼저 처리했습니다."),
    STORE_APPROVAL_INVALID_TRANSITION(HttpStatus.CONFLICT, "STORE_APPROVAL_INVALID_TRANSITION", "현재 상태에서는 요청한 처리를 할 수 없습니다."),
    STORE_APPROVAL_DOCUMENT_INCOMPLETE(HttpStatus.CONFLICT, "STORE_APPROVAL_DOCUMENT_INCOMPLETE", "필수 문서 검수가 완료되지 않았습니다."),
    STORE_APPROVAL_VERIFICATION_INCOMPLETE(HttpStatus.CONFLICT, "STORE_APPROVAL_VERIFICATION_INCOMPLETE", "사업자 인증이 완료되지 않았습니다."),
    STORE_APPROVAL_DUPLICATE_STORE(HttpStatus.CONFLICT, "STORE_APPROVAL_DUPLICATE_STORE", "동일한 사업자등록번호의 매장이 이미 존재합니다."),
    STORE_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_DOCUMENT_NOT_FOUND", "신청 문서를 찾을 수 없습니다."),

    BUSINESS_NUMBER_INVALID(HttpStatus.BAD_REQUEST, "BUSINESS_NUMBER_INVALID", "사업자등록번호 형식이 올바르지 않습니다."),
    STORE_APPLICATION_DUPLICATE_BUSINESS(HttpStatus.CONFLICT, "STORE_APPLICATION_DUPLICATE_BUSINESS", "동일한 사업자등록번호의 진행 중인 신청이 있습니다."),
    BUSINESS_VERIFICATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BUSINESS_VERIFICATION_UNAVAILABLE", "사업자 정보를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.");

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
