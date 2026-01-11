package com.plateapp.plate_main.auth.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileHistoryResponse {
    long historyId;
    LocalDateTime loggedAt;
}
