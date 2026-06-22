package com.plateapp.plate_main.admin.feedmoderation.service;

import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.feedmoderation.dto.AdminFeedDtos.ActionRequest;
import com.plateapp.plate_main.admin.feedmoderation.dto.AdminFeedDtos.RecommendationRequest;
import com.plateapp.plate_main.admin.feedmoderation.dto.AdminFeedDtos.ReportResponse;
import com.plateapp.plate_main.admin.feedmoderation.dto.AdminFeedDtos.Response;
import com.plateapp.plate_main.admin.feedmoderation.entity.AdminFeedModeration;
import com.plateapp.plate_main.admin.feedmoderation.repository.AdminFeedModerationRepository;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.report.repository.ReportRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFeedService {
    private final Fp400FeedRepository feedRepository;
    private final AdminFeedModerationRepository moderationRepository;
    private final ReportRepository reports;
    private final AdminAuditService audit;

    @Transactional(readOnly = true)
    public AdminPageResponse<Response> list(
            int page,
            int size,
            String keyword,
            String visibility,
            Boolean recommended
    ) {
        validatePage(page, size);
        String normalizedVisibility = normalize(visibility);
        String useYn = normalizedVisibility == null ? null : switch (normalizedVisibility) {
            case "visible" -> "Y";
            case "hidden" -> "N";
            default -> throw invalid("Unsupported visibility.");
        };
        return AdminPageResponse.from(moderationRepository.searchFeeds(
                trim(keyword), useYn, recommended,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "feedNo"))
        ).map(this::map));
    }

    @Transactional(readOnly = true)
    public Response detail(Integer id) {
        return map(findFeed(id));
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<ReportResponse> reports(Integer id, int page, int size) {
        findFeed(id);
        validatePage(page, size);
        return AdminPageResponse.from(reports.findByTargetTypeIgnoreCaseAndTargetId(
                "FEED", id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt", "id"))
        ).map(report -> new ReportResponse(
                report.getId(), report.getReporterUsername(), report.getReporterUserId(), report.getReason(),
                report.getSubmittedAt(), report.getUnflaggedAt() == null ? "open" : "cleared"
        )));
    }

    @Transactional
    public Response visibility(
            Integer id,
            ActionRequest command,
            boolean visible,
            AdminActor actor,
            HttpServletRequest request
    ) {
        Fp400Feed feed = findFeed(id);
        AdminFeedModeration state = stateForUpdate(feed, command.version());
        String previous = state.getVisibilityStatus();
        String next = visible ? "visible" : "hidden";
        state.changeVisibility(next, command.reason().trim(), actor.userId());
        state = moderationRepository.saveAndFlush(state);
        feedRepository.updateAdminVisibility(id, visible ? "Y" : "N");
        feed = findFeed(id);

        audit.record(actor, visible ? "FEED_RESTORED" : "FEED_HIDDEN", "FEED", id,
                Map.of("visibility", previous),
                Map.of("visibility", next, "version", state.getVersion()),
                null, command.reason(), request);
        return map(feed, state);
    }

    @Transactional
    public Response recommendation(
            Integer id,
            RecommendationRequest command,
            AdminActor actor,
            HttpServletRequest request
    ) {
        Fp400Feed feed = findFeed(id);
        AdminFeedModeration state = stateForUpdate(feed, command.version());
        boolean previous = Boolean.TRUE.equals(state.getRecommended());
        state.changeRecommendation(command.recommended(), command.reason().trim(), actor.userId());
        state = moderationRepository.saveAndFlush(state);
        audit.record(actor, "FEED_RECOMMENDATION_CHANGED", "FEED", id,
                Map.of("recommended", previous),
                Map.of("recommended", command.recommended(), "version", state.getVersion()),
                null, command.reason(), request);
        return map(feed, state);
    }

    private Fp400Feed findFeed(Integer id) {
        return feedRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));
    }

    private AdminFeedModeration stateForUpdate(Fp400Feed feed, Long expectedVersion) {
        AdminFeedModeration state = moderationRepository.findById(feed.getFeedNo())
                .orElseGet(() -> AdminFeedModeration.initial(
                        feed.getFeedNo(), "Y".equalsIgnoreCase(feed.getUseYn()) ? "visible" : "hidden"
                ));
        if (!Objects.equals(expectedVersion, state.getVersion())) {
            throw new AppException(ErrorCode.COMMON_CONFLICT);
        }
        return state;
    }

    private Response map(Fp400Feed feed) {
        AdminFeedModeration state = moderationRepository.findById(feed.getFeedNo())
                .orElseGet(() -> AdminFeedModeration.initial(
                        feed.getFeedNo(), "Y".equalsIgnoreCase(feed.getUseYn()) ? "visible" : "hidden"
                ));
        return map(feed, state);
    }

    private Response map(Fp400Feed feed, AdminFeedModeration state) {
        return new Response(feed.getFeedNo(), feed.getUsername(), feed.getFeedTitle(), feed.getContent(),
                feed.getImages(), feed.getStoreName(), feed.getPlaceId(), state.getVisibilityStatus(),
                Boolean.TRUE.equals(state.getRecommended()),
                reports.countByTargetTypeIgnoreCaseAndTargetId("FEED", feed.getFeedNo()),
                state.getReason(), state.getModeratedBy(), state.getModeratedAt(),
                feed.getCreatedAt(), feed.getUpdatedAt(), state.getVersion());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw invalid("Invalid page or size.");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        String result = trim(value);
        return result == null ? null : result.toLowerCase(Locale.ROOT);
    }

    private AppException invalid(String message) {
        return new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
    }
}
