package com.plateapp.plate_main.report.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.report.dto.ReportCreateRequest;
import com.plateapp.plate_main.report.dto.ReportHistoryItem;
import com.plateapp.plate_main.report.dto.ReportHistoryResponse;
import com.plateapp.plate_main.report.entity.Fp40Report;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> ALLOWED_TARGET_TYPES =
            Set.of("video", "image", "comment", "user");
    private final ReportRepository reportRepository;
    private final Fp300StoreRepository fp300StoreRepository;
    private final ImageFeedRepository imageFeedRepository;

    @Transactional
    public Integer createReport(String reporterUsername, ReportCreateRequest request) {
        if (reporterUsername == null || reporterUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthenticated");
        }

        String targetType = request.getTargetType().trim().toLowerCase();
        if (!ALLOWED_TARGET_TYPES.contains(targetType)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "invalid targetType");
        }

        String reason = request.getReason().trim();
        if (reason.isEmpty()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "invalid reason");
        }

        Fp40Report report = new Fp40Report();
        report.setReporterUsername(reporterUsername);
        report.setTargetUsername(trimToNull(request.getTargetUsername()));
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReason(reason);
        report.setSubmittedAt(LocalDateTime.now());
        report.setTargetFlag("Y");

        return reportRepository.save(report).getId();
    }

    @Transactional(readOnly = true)
    public ReportHistoryResponse listReports(String reporterUsername, int limit, int offset) {
        if (reporterUsername == null || reporterUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthenticated");
        }

        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(offset, 0);
        PageRequest pageable = PageRequest.of(
                safeOffset / safeLimit,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "submittedAt")
        );

        Page<Fp40Report> page = reportRepository.findByReporterUsername(reporterUsername, pageable);
        Map<Integer, TargetInfo> videoInfo = loadVideoInfo(page.getContent());
        Map<Integer, TargetInfo> imageInfo = loadImageInfo(page.getContent());

        List<ReportHistoryItem> items = new ArrayList<>();
        for (Fp40Report report : page.getContent()) {
            String description = null;
            TargetInfo info = null;
            if ("video".equalsIgnoreCase(report.getTargetType())) {
                info = videoInfo.get(report.getTargetId());
            } else if ("image".equalsIgnoreCase(report.getTargetType())) {
                info = imageInfo.get(report.getTargetId());
            }
            items.add(ReportHistoryItem.builder()
                    .reportId(report.getId())
                    .targetType(report.getTargetType())
                    .targetId(report.getTargetId())
                    .targetUsername(report.getTargetUsername())
                    .reason(report.getReason())
                    .description(description)
                    .placeId(info != null ? info.placeId : null)
                    .storeName(info != null ? info.storeName : null)
                    .thumbnail(info != null ? info.thumbnail : null)
                    .status(statusLabel(report.getTargetFlag(), report.getUnflaggedAt()))
                    .createdAt(report.getSubmittedAt())
                    .resolvedAt(report.getUnflaggedAt())
                    .build());
        }

        return ReportHistoryResponse.builder()
                .items(items)
                .total(page.getTotalElements())
                .offset(safeOffset)
                .limit(safeLimit)
                .build();
    }

    private String statusLabel(String flag, LocalDateTime unflaggedAt) {
        if (unflaggedAt != null) {
            return "DONE";
        }
        if (flag == null) {
            return "RECEIVED";
        }
        if ("Y".equalsIgnoreCase(flag)) {
            return "RECEIVED";
        }
        if ("N".equalsIgnoreCase(flag)) {
            return "DONE";
        }
        return flag;
    }

    private Map<Integer, TargetInfo> loadVideoInfo(List<Fp40Report> reports) {
        List<Integer> ids = new ArrayList<>();
        for (Fp40Report report : reports) {
            if ("video".equalsIgnoreCase(report.getTargetType()) && report.getTargetId() != null) {
                ids.add(report.getTargetId());
            }
        }
        Map<Integer, TargetInfo> labels = new HashMap<>();
        if (ids.isEmpty()) {
            return labels;
        }
        for (Fp300Store store : fp300StoreRepository.findByStoreIdIn(ids)) {
            String storeName = store.getStoreName();
            if (storeName == null || storeName.isBlank()) {
                storeName = store.getTitle();
            }
            labels.put(store.getStoreId(), new TargetInfo(
                    store.getPlaceId(),
                    storeName,
                    store.getThumbnail()
            ));
        }
        return labels;
    }

    private Map<Integer, TargetInfo> loadImageInfo(List<Fp40Report> reports) {
        List<Integer> ids = new ArrayList<>();
        for (Fp40Report report : reports) {
            if ("image".equalsIgnoreCase(report.getTargetType()) && report.getTargetId() != null) {
                ids.add(report.getTargetId());
            }
        }
        Map<Integer, TargetInfo> labels = new HashMap<>();
        if (ids.isEmpty()) {
            return labels;
        }
        for (Fp400ImageFeed feed : imageFeedRepository.findByFeedIdIn(ids)) {
            String storeName = feed.getStoreName();
            if (storeName == null || storeName.isBlank()) {
                storeName = feed.getFeedTitle();
            }
            if (storeName == null || storeName.isBlank()) {
                storeName = feed.getLocation();
            }
            labels.put(feed.getFeedId(), new TargetInfo(
                    feed.getPlaceId(),
                    storeName,
                    feed.getThumbnail()
            ));
        }
        return labels;
    }

    private static final class TargetInfo {
        private final String placeId;
        private final String storeName;
        private final String thumbnail;

        private TargetInfo(String placeId, String storeName, String thumbnail) {
            this.placeId = placeId;
            this.storeName = storeName;
            this.thumbnail = thumbnail;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
