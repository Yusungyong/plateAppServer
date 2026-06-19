package com.plateapp.plate_main.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.auth.exception.AccountConflictException;
import com.plateapp.plate_main.common.api.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerAccountConflictTest {

    @Test
    void accountConflictReturns409WithFieldErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Map<String, String> fieldErrors = Map.of(
                "email", "이미 가입된 이메일입니다."
        );

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                handler.handleAccountConflict(new AccountConflictException(fieldErrors));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCOUNT_CONFLICT");
        assertThat(response.getBody().getData().get("fieldErrors")).isEqualTo(fieldErrors);
    }
}
