package com.plateapp.plate_main.home.dto;

import java.time.LocalDateTime;

public record HomeImpressionResponse(
        int savedCount,
        int duplicateCount,
        LocalDateTime suppressUntil
) {
}
