package com.plateapp.plate_main.admin.membermonitoring.dto;

import java.time.LocalDateTime;

public record ProfileChangeItem(
    long historyId,
    String username,
    String changedField,
    String actor,
    LocalDateTime createdAt
) {
}
