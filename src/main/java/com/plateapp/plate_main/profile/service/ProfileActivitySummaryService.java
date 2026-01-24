package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.profile.dto.ProfileActivitySummaryResponse;
import com.plateapp.plate_main.profile.dto.ProfileDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileActivitySummaryService {

    private final ProfileDetailService profileDetailService;

    @Transactional(readOnly = true)
    public ProfileActivitySummaryResponse getPublicSummary(String username) {
        ProfileDetailResponse.Stats stats = profileDetailService.loadStats(username);
        return ProfileActivitySummaryResponse.builder()
                .username(username)
                .stats(stats)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileActivitySummaryResponse getMySummary(String username) {
        ProfileDetailResponse.Stats stats = profileDetailService.loadStats(username);
        return ProfileActivitySummaryResponse.builder()
                .username(username)
                .stats(stats)
                .build();
    }
}
