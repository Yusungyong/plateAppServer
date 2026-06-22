package com.plateapp.plate_main.admin.feedback.service;

import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.feedback.dto.FeedbackDtos;
import com.plateapp.plate_main.admin.feedback.entity.ServiceFeedback;
import com.plateapp.plate_main.admin.feedback.repository.ServiceFeedbackRepository;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

@Service @RequiredArgsConstructor
public class FeedbackService {
    private static final Set<String> STATUSES = Set.of("received", "in_progress", "resolved", "improvement_candidate");
    private final ServiceFeedbackRepository repository;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    @Value("${feedback.contact-retention-days:90}")
    private int contactRetentionDays;

    @Transactional
    public FeedbackDtos.Response create(FeedbackDtos.CreateRequest request, String username) {
        Integer userId = Optional.ofNullable(username).flatMap(userRepository::findById).map(User::getUserId).orElse(null);
        return map(repository.save(ServiceFeedback.create(normalizeRequired(request.type()).toLowerCase(Locale.ROOT),
                requiredText(request.content()), trim(request.contact()), userId, contactRetentionDays)));
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<FeedbackDtos.Response> list(int page, int size, String keyword, String type,
            String status, LocalDate from, LocalDate to) {
        validatePage(page, size);
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) throw invalid("Unsupported feedback status.");
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        Page<ServiceFeedback> result = repository.search(normalize(keyword), normalize(type), normalizedStatus,
                from == null ? null : from.atStartOfDay(seoul).toOffsetDateTime(),
                to == null ? null : to.plusDays(1).atStartOfDay(seoul).toOffsetDateTime(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return AdminPageResponse.from(result.map(this::map));
    }

    @Transactional(readOnly = true)
    public FeedbackDtos.Summary summary() {
        return new FeedbackDtos.Summary(repository.count(), repository.countByStatus("received"),
                repository.countByStatus("in_progress"), repository.countByStatus("resolved"),
                repository.countByStatus("improvement_candidate"));
    }

    @Scheduled(cron = "${feedback.contact-purge-cron:0 30 3 * * *}", zone = "UTC")
    @Transactional
    public int purgeExpiredContacts() {
        return repository.purgeExpiredContacts(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public FeedbackDtos.Response update(Long id, FeedbackDtos.UpdateRequest command, AdminActor actor,
            HttpServletRequest request) {
        ServiceFeedback feedback = repository.findById(id).orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));
        if (!Objects.equals(command.version(), feedback.getVersion())) throw new AppException(ErrorCode.COMMON_CONFLICT);
        String status = normalize(command.status());
        if (status != null && !STATUSES.contains(status)) throw invalid("Unsupported feedback status.");
        Map<String, Object> previous = Map.of("status", feedback.getStatus(), "version", feedback.getVersion());
        feedback.update(status, command.assigneeUserId(), trim(command.internalMemo()));
        feedback = repository.saveAndFlush(feedback);
        auditService.record(actor, "FEEDBACK_UPDATED", "SERVICE_FEEDBACK", id, previous,
                Map.of("status", feedback.getStatus(), "version", feedback.getVersion()), null, null, request);
        return map(feedback);
    }

    private FeedbackDtos.Response map(ServiceFeedback f) {
        return new FeedbackDtos.Response(f.getId(), f.getType(), f.getContent(), f.getStatus(), f.getContact(), f.getContactPurgeAt(),
                f.getRequesterUserId(), f.getAssigneeUserId(), f.getInternalMemo(), f.getCreatedAt(), f.getUpdatedAt(), f.getVersion());
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT); }
    private String normalizeRequired(String value) { String result = normalize(value); if (result == null) throw invalid("Required value is blank."); return result; }
    private String requiredText(String value) { String result = trim(value); if (result == null) throw invalid("Required value is blank."); return result; }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void validatePage(int page, int size) { if (page < 0 || size < 1 || size > 100) throw invalid("Invalid page or size."); }
    private AppException invalid(String message) { return new AppException(ErrorCode.COMMON_INVALID_INPUT, message); }
}
