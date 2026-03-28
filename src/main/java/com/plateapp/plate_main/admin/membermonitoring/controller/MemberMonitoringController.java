package com.plateapp.plate_main.admin.membermonitoring.controller;

import com.plateapp.plate_main.admin.membermonitoring.dto.LoginRiskResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.MemberMonitoringSummaryResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.ProfileChangeResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.RiskUserResponse;
import com.plateapp.plate_main.admin.membermonitoring.service.MemberMonitoringService;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-monitoring")
@RequiredArgsConstructor
public class MemberMonitoringController {

    private final MemberMonitoringService memberMonitoringService;

    @GetMapping("/summary")
    public ResponseEntity<MemberMonitoringSummaryResponse> getSummary(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(memberMonitoringService.getSummary());
    }

    @GetMapping("/login-risks")
    public ResponseEntity<LoginRiskResponse> getLoginRisks(
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(memberMonitoringService.getLoginRisks(limit));
    }

    @GetMapping("/profile-changes")
    public ResponseEntity<ProfileChangeResponse> getProfileChanges(
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(memberMonitoringService.getProfileChanges(limit));
    }

    @GetMapping("/risk-users")
    public ResponseEntity<RiskUserResponse> getRiskUsers(
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(memberMonitoringService.getRiskUsers(limit));
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Admin role required");
        }
    }
}
