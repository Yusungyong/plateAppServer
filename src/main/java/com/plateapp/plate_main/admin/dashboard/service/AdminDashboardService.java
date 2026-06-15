package com.plateapp.plate_main.admin.dashboard.service;

import com.plateapp.plate_main.admin.audit.entity.AdminAuditLog;
import com.plateapp.plate_main.admin.audit.repository.AdminAuditLogRepository;
import com.plateapp.plate_main.admin.dashboard.dto.AdminDashboardDtos;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final NamedParameterJdbcTemplate jdbc;
    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDtos.SummaryResponse summary(LocalDate from, LocalDate to) {
        DateRange current = range(from, to);
        DateRange previous = previousRange(current);

        long newApplications = count("""
                select count(*)
                from store_applications
                where applied_at >= :fromInstant and applied_at < :toInstant
                """, current);
        long previousApplications = count("""
                select count(*)
                from store_applications
                where applied_at >= :fromInstant and applied_at < :toInstant
                """, previous);
        long pendingApprovals = scalar("select count(*) from store_applications where approval_status = 'pending'");
        long activeStores = count("""
                select count(*)
                from restaurants
                where updated_at >= :fromInstant and updated_at < :toInstant
                """, current);
        long userReports = countLocal("""
                select count(*)
                from fp_40
                where submitted_at >= :fromLocal and submitted_at < :toLocal
                """, current);
        long regionalPosts = countLocal("""
                select count(*)
                from fp_400
                where created_at >= :fromLocal and created_at < :toLocal
                  and location is not null and trim(location) <> ''
                """, current);

        return new AdminDashboardDtos.SummaryResponse(List.of(
                metric("newStoreApplications", "신규 매장 신청", newApplications,
                        changeRate(newApplications, previousApplications), "previous_period"),
                metric("pendingApprovals", "승인 대기", pendingApprovals, null, "current"),
                metric("activeStores", "활성 매장", activeStores, null, "current_period"),
                metric("userReports", "사용자 신고", userReports, null, "current_period"),
                metric("seasonalMenus", "제철 메뉴", 0, null, "not_available"),
                metric("regionalPosts", "지역별 게시물", regionalPosts, null, "current_period")
        ));
    }

    @Transactional(readOnly = true)
    public List<AdminDashboardDtos.ActivityTrend> trends(LocalDate from, LocalDate to, String interval) {
        DateRange validated = range(from, to);
        if (interval != null && !"day".equalsIgnoreCase(interval)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "interval must be day.");
        }
        if (Duration.between(validated.from().atStartOfDay(), validated.toExclusive().atStartOfDay()).toDays() > 93) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "activity trend range must be 93 days or less.");
        }

        List<AdminDashboardDtos.ActivityTrend> result = new ArrayList<>();
        for (LocalDate date = validated.from(); date.isBefore(validated.toExclusive()); date = date.plusDays(1)) {
            DateRange day = new DateRange(date, date.plusDays(1));
            long activeStores = count("""
                    select count(*)
                    from restaurants
                    where updated_at >= :fromInstant and updated_at < :toInstant
                    """, day);
            long posts = countLocal("""
                    select count(*)
                    from fp_400
                    where created_at >= :fromLocal and created_at < :toLocal
                    """, day);
            long reactions = countLocal("""
                    select
                        (select count(*) from fp_50 where created_at >= :fromLocal and created_at < :toLocal)
                      + (select count(*) from fp_60 where created_at >= :fromLocal and created_at < :toLocal)
                      + (select count(*) from fp_440 where created_at >= :fromLocal and created_at < :toLocal)
                      + (select count(*) from fp_460 where created_at >= :fromLocal and created_at < :toLocal)
                    """, day);
            result.add(new AdminDashboardDtos.ActivityTrend(date, activeStores, posts, reactions));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AdminDashboardDtos.RegionDistribution> regionDistribution(LocalDate from, LocalDate to) {
        DateRange dateRange = range(from, to);
        MapSqlParameterSource params = localParams(dateRange);
        return jdbc.query("""
                select upper(trim(location)) as region_code,
                       trim(location) as region_name,
                       count(*) as post_count
                from fp_400
                where created_at >= :fromLocal and created_at < :toLocal
                  and location is not null and trim(location) <> ''
                group by upper(trim(location)), trim(location)
                order by post_count desc, region_code asc
                """, params, (rs, rowNum) -> new AdminDashboardDtos.RegionDistribution(
                rs.getString("region_code"),
                rs.getString("region_name"),
                rs.getLong("post_count")
        ));
    }

    @Transactional(readOnly = true)
    public AdminDashboardDtos.ActivityListResponse activities(int page, int size, String sort) {
        validatePage(page, size);
        Page<AdminAuditLog> result = auditLogRepository.findAll(
                PageRequest.of(page, size, activitySort(sort))
        );
        List<AdminDashboardDtos.ActivityItem> content = result.getContent().stream()
                .map(this::activity)
                .toList();
        return new AdminDashboardDtos.ActivityListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private AdminDashboardDtos.ActivityItem activity(AdminAuditLog log) {
        User operator = userRepository.findByUserId(log.getActorUserId()).orElse(null);
        return new AdminDashboardDtos.ActivityItem(
                log.getId(),
                log.getOccurredAt(),
                log.getResourceType(),
                log.getResourceId(),
                value(log.getNextValue(), "storeName"),
                log.getAction(),
                actionLabel(log.getAction()),
                log.getActorUserId(),
                operator == null ? null : operator.getNickname(),
                value(log.getNextValue(), "approvalStatus")
        );
    }

    private AdminDashboardDtos.Metric metric(
            String key,
            String label,
            long value,
            Double changeRate,
            String comparison
    ) {
        return new AdminDashboardDtos.Metric(key, label, value, changeRate, comparison);
    }

    private Double changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : null;
        }
        return Math.round(((current - previous) * 10000.0 / previous)) / 100.0;
    }

    private long count(String sql, DateRange range) {
        return number(jdbc.queryForObject(sql, instantParams(range), Number.class));
    }

    private long countLocal(String sql, DateRange range) {
        return number(jdbc.queryForObject(sql, localParams(range), Number.class));
    }

    private long scalar(String sql) {
        return number(jdbc.getJdbcTemplate().queryForObject(sql, Number.class));
    }

    private long number(Number value) {
        return value == null ? 0 : value.longValue();
    }

    private MapSqlParameterSource instantParams(DateRange range) {
        return new MapSqlParameterSource()
                .addValue("fromInstant", range.from().atStartOfDay(SEOUL).toOffsetDateTime())
                .addValue("toInstant", range.toExclusive().atStartOfDay(SEOUL).toOffsetDateTime());
    }

    private MapSqlParameterSource localParams(DateRange range) {
        return new MapSqlParameterSource()
                .addValue("fromLocal", range.from().atStartOfDay())
                .addValue("toLocal", range.toExclusive().atStartOfDay());
    }

    private DateRange range(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "from and to are required.");
        }
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "from must be on or before to.");
        }
        return new DateRange(from, to.plusDays(1));
    }

    private DateRange previousRange(DateRange current) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(current.from(), current.toExclusive());
        return new DateRange(current.from().minusDays(days), current.from());
    }

    private Sort activitySort(String rawSort) {
        String value = rawSort == null ? "occurredAt,desc" : rawSort.trim();
        String[] parts = value.split(",");
        if (parts.length != 2 || !"occurredAt".equals(parts[0].trim())) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Only occurredAt sort is supported.");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "sort direction must be asc or desc.");
        }
        return Sort.by(new Sort.Order(direction, "occurredAt"), Sort.Order.desc("id"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "page must be >= 0 and size must be between 1 and 100.");
        }
    }

    private String value(Map<String, Object> values, String key) {
        if (values == null || values.get(key) == null) {
            return null;
        }
        return String.valueOf(values.get(key));
    }

    private String actionLabel(String action) {
        if (action == null) {
            return null;
        }
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "STORE_APPROVED" -> "매장 승인";
            case "STORE_HELD" -> "매장 보류";
            case "STORE_REJECTED" -> "매장 반려";
            case "STORE_APPROVAL_VIEWED" -> "매장 신청 조회";
            case "STORE_DOCUMENT_ACCESS_URL_ISSUED" -> "신청 문서 접근";
            default -> action;
        };
    }

    private record DateRange(LocalDate from, LocalDate toExclusive) {
    }
}
