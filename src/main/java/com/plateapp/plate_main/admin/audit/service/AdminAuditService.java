package com.plateapp.plate_main.admin.audit.service;

import com.plateapp.plate_main.admin.audit.entity.AdminAuditLog;
import com.plateapp.plate_main.admin.audit.repository.AdminAuditLogRepository;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.common.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public void record(
            AdminActor actor,
            String action,
            String resourceType,
            Object resourceId,
            Map<String, Object> previousValue,
            Map<String, Object> nextValue,
            String reasonCode,
            String reason,
            HttpServletRequest request
    ) {
        auditLogRepository.save(AdminAuditLog.create(
                OffsetDateTime.now(ZoneOffset.UTC),
                actor.userId(),
                actor.role(),
                action,
                resourceType,
                String.valueOf(resourceId),
                previousValue,
                nextValue,
                reasonCode,
                reason,
                clientIp(request),
                header(request, "User-Agent"),
                MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)
        ));
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }
}
