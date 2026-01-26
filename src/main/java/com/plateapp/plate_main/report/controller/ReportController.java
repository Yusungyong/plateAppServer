package com.plateapp.plate_main.report.controller;

import com.plateapp.plate_main.report.dto.ReportCreateRequest;
import com.plateapp.plate_main.report.dto.ReportCreateResponse;
import com.plateapp.plate_main.report.dto.ReportHistoryResponse;
import com.plateapp.plate_main.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/reports")
    public ResponseEntity<ReportCreateResponse> createReport(
            @RequestBody @Valid ReportCreateRequest request
    ) {
        String username = currentUsername();
        Integer reportId = reportService.createReport(username, request);
        return ResponseEntity.ok(ReportCreateResponse.builder()
                .ok(true)
                .reportId(reportId)
                .build());
    }

    @GetMapping("/reports")
    public ResponseEntity<ReportHistoryResponse> listReports(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        String username = currentUsername();
        ReportHistoryResponse response = reportService.listReports(username, limit, offset);
        return ResponseEntity.ok(response);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
                return name;
            }
        }
        return null;
    }
}
