package com.plateapp.plate_main.admin.membermonitoring.dto;

import java.util.List;

public record RiskUserResponse(
    List<RiskUserItem> items
) {
}
