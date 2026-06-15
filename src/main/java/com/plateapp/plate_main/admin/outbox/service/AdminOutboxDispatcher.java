package com.plateapp.plate_main.admin.outbox.service;

import com.plateapp.plate_main.admin.outbox.entity.AdminOutboxEvent;
import com.plateapp.plate_main.admin.outbox.repository.AdminOutboxEventRepository;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.service.NotificationCommandService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOutboxDispatcher {

    private final AdminOutboxEventRepository outboxRepository;
    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelayString = "${admin.outbox.fixed-delay-ms:5000}")
    public void dispatchPending() {
        List<Long> eventIds = outboxRepository
                .findByStatusAndAvailableAtLessThanEqualOrderByIdAsc(
                        "pending",
                        OffsetDateTime.now(ZoneOffset.UTC),
                        PageRequest.of(0, 20)
                )
                .stream()
                .map(AdminOutboxEvent::getId)
                .toList();
        for (Long eventId : eventIds) {
            dispatchOne(eventId);
        }
    }

    private void dispatchOne(Long eventId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            AdminOutboxEvent event = outboxRepository.findByIdForUpdate(eventId);
            if (event == null || !"pending".equals(event.getStatus())) {
                return;
            }
            try {
                dispatch(event);
                event.markProcessed(OffsetDateTime.now(ZoneOffset.UTC));
            } catch (RuntimeException e) {
                int nextAttempt = event.getAttemptCount() + 1;
                event.markRetry(
                        OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(backoffSeconds(nextAttempt)),
                        e.getMessage()
                );
                log.warn("Admin outbox dispatch failed. eventId={} attempt={}", eventId, nextAttempt, e);
            }
        });
    }

    private void dispatch(AdminOutboxEvent event) {
        if (!"STORE_APPROVAL".equals(event.getAggregateType())) {
            throw new IllegalArgumentException("Unsupported admin outbox aggregate type");
        }
        Map<String, Object> payload = event.getPayload();
        Integer applicantUserId = number(payload.get("applicantUserId")).intValue();
        Long applicationId = number(payload.get("applicationId")).longValue();
        String approvalStatus = String.valueOf(payload.get("approvalStatus"));
        String actorUsername = String.valueOf(payload.get("actorUsername"));
        User receiver = userRepository.findByUserId(applicantUserId)
                .orElseThrow(() -> new IllegalStateException("Outbox receiver not found"));
        notificationCommandService.notifyStoreApprovalStatus(
                actorUsername,
                receiver.getUsername(),
                applicationId,
                approvalStatus
        );
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private long backoffSeconds(int attempt) {
        return Math.min(300L, 5L * (1L << Math.max(0, attempt - 1)));
    }
}
