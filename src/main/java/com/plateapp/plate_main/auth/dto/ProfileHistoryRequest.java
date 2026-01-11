package com.plateapp.plate_main.auth.dto;

import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileHistoryRequest {
    @NotBlank
    String changeType;          // 코드값 (예: CD_008 등)
    Map<String, Object> before; // 이전 상태 (JSON)
    Map<String, Object> after;  // 변경 후 상태 (JSON)
    String memo;                // 선택 메모
}
