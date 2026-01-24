package com.plateapp.plate_main.profile.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileActivitySummaryResponse {
    String username;
    ProfileDetailResponse.Stats stats;
}
