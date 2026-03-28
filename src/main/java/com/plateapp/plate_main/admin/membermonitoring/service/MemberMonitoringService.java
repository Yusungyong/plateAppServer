package com.plateapp.plate_main.admin.membermonitoring.service;

import com.plateapp.plate_main.admin.membermonitoring.dto.LoginRiskItem;
import com.plateapp.plate_main.admin.membermonitoring.dto.LoginRiskResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.MemberMonitoringSummaryResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.ProfileChangeItem;
import com.plateapp.plate_main.admin.membermonitoring.dto.ProfileChangeResponse;
import com.plateapp.plate_main.admin.membermonitoring.dto.RiskUserItem;
import com.plateapp.plate_main.admin.membermonitoring.dto.RiskUserResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberMonitoringService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public MemberMonitoringSummaryResponse getSummary() {
        long totalUsers = queryLong("select count(*) from fp_100");
        long newUsersToday = queryLong("select count(*) from fp_100 where created_at = current_date");
        long activeUsers7d = queryLong("""
            select count(distinct username)
            from fp_105
            where username is not null
              and upper(coalesce(login_status, '')) = 'SUCCESS'
              and login_datetime >= current_timestamp - interval '7 days'
            """);
        double loginFailureRateToday = queryDouble("""
            select coalesce(
                round(
                    100.0 * sum(case when upper(coalesce(login_status, '')) in ('FAIL', 'FAILED') then 1 else 0 end)
                    / nullif(count(*), 0),
                    1
                ),
                0
            )
            from fp_105
            where login_datetime >= date_trunc('day', current_timestamp)
            """);
        long pendingRoleChanges = queryLong("""
            select count(*)
            from fp_101
            where created_dt >= current_timestamp - interval '24 hours'
              and (
                upper(coalesce(change_tp, '')) in ('CD_004', 'ROLE_CHANGED', 'ROLE_CHANGE')
                or upper(coalesce(before_ex, '')) like '%ROLE%'
                or upper(coalesce(after_ex, '')) like '%ROLE%'
              )
            """);
        long riskUsers24h = queryLong("""
            select count(distinct username)
            from (
                select username
                from fp_105
                where username is not null
                  and login_datetime >= current_timestamp - interval '24 hours'
                  and upper(coalesce(login_status, '')) in ('FAIL', 'FAILED')
                group by username
                having count(*) >= 5

                union

                select target_username as username
                from fp_40
                where target_username is not null
                  and submitted_at >= current_timestamp - interval '24 hours'

                union

                select blocked_username as username
                from fp_160
                where blocked_username is not null
                  and blocked_at >= current_timestamp - interval '24 hours'
            ) risky
            """);

        return new MemberMonitoringSummaryResponse(
            totalUsers,
            newUsersToday,
            activeUsers7d,
            loginFailureRateToday,
            pendingRoleChanges,
            riskUsers24h
        );
    }

    @Transactional(readOnly = true)
    public LoginRiskResponse getLoginRisks(int limit) {
        int safeLimit = normalizeLimit(limit);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);

        List<LoginRiskCandidate> items = new ArrayList<>();
        items.addAll(jdbcTemplate.query("""
            select
              username,
              coalesce(ip_address, '') as ip_address,
              coalesce(device_id, '') as device_id,
              count(*) as failure_count,
              max(login_datetime) as last_occurred_at
            from fp_105
            where username is not null
              and login_datetime >= current_timestamp - interval '24 hours'
              and upper(coalesce(login_status, '')) in ('FAIL', 'FAILED')
            group by username, ip_address, device_id
            having count(*) >= 3
            order by failure_count desc, last_occurred_at desc
            limit :limit
            """, params, (rs, rowNum) -> {
            int failureCount = rs.getInt("failure_count");
            LocalDateTime lastOccurredAt = toLocalDateTime(rs.getTimestamp("last_occurred_at"));
            String ipAddress = blankToNull(rs.getString("ip_address"));
            return new LoginRiskCandidate(
                rs.getString("username"),
                "LOGIN_FAILURE_BURST",
                "연속 로그인 실패",
                String.format("최근 24시간 동안 %d회 실패%s", failureCount, ipAddress != null ? ", 동일 IP " + ipAddress : ""),
                ipAddress,
                blankToNull(rs.getString("device_id")),
                Math.min(100, 55 + (failureCount * 4)),
                lastOccurredAt
            );
        }));

        items.addAll(jdbcTemplate.query("""
            select
              ip_address,
              count(distinct username) as user_count,
              string_agg(distinct username, ', ' order by username) as usernames,
              max(login_datetime) as last_occurred_at
            from fp_105
            where username is not null
              and ip_address is not null
              and ip_address <> ''
              and login_datetime >= current_timestamp - interval '24 hours'
            group by ip_address
            having count(distinct username) >= 3
            order by user_count desc, last_occurred_at desc
            limit :limit
            """, params, (rs, rowNum) -> {
            int userCount = rs.getInt("user_count");
            String usernames = summarizeUsernames(rs.getString("usernames"));
            return new LoginRiskCandidate(
                usernames,
                "MULTI_ACCOUNT_IP",
                "동일 IP 다계정 접근",
                String.format("최근 24시간 동안 동일 IP에서 %d개 계정 로그인/시도", userCount),
                rs.getString("ip_address"),
                null,
                Math.min(100, 45 + (userCount * 8)),
                toLocalDateTime(rs.getTimestamp("last_occurred_at"))
            );
        }));

        List<LoginRiskItem> results = items.stream()
            .sorted(Comparator
                .comparingInt(LoginRiskCandidate::score).reversed()
                .thenComparing(LoginRiskCandidate::lastOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(safeLimit)
            .map(item -> new LoginRiskItem(
                item.username(),
                item.riskType(),
                item.riskLabel(),
                item.detail(),
                item.ipAddress(),
                item.deviceId(),
                item.score(),
                item.lastOccurredAt()
            ))
            .toList();

        return new LoginRiskResponse(results);
    }

    @Transactional(readOnly = true)
    public ProfileChangeResponse getProfileChanges(int limit) {
        int safeLimit = normalizeLimit(limit);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);
        List<ProfileChangeItem> items = jdbcTemplate.query("""
            select history_id, username, change_tp, before_ex, after_ex, created_dt
            from fp_101
            order by created_dt desc nulls last, history_id desc
            limit :limit
            """, params, (rs, rowNum) -> {
            String changeTp = rs.getString("change_tp");
            String beforeEx = rs.getString("before_ex");
            String afterEx = rs.getString("after_ex");
            return new ProfileChangeItem(
                rs.getLong("history_id"),
                rs.getString("username"),
                mapChangedField(changeTp, beforeEx, afterEx),
                "system",
                toLocalDateTime(rs.getTimestamp("created_dt"))
            );
        });
        return new ProfileChangeResponse(items);
    }

    @Transactional(readOnly = true)
    public RiskUserResponse getRiskUsers(int limit) {
        int safeLimit = normalizeLimit(limit);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", safeLimit);
        List<RiskUserItem> items = jdbcTemplate.query("""
            with reports as (
                select target_username as username, count(*) as report_count
                from fp_40
                where target_username is not null
                  and target_username <> ''
                  and submitted_at >= current_timestamp - interval '30 days'
                group by target_username
            ),
            blocks as (
                select blocked_username as username, count(*) as blocked_count
                from fp_160
                where blocked_username is not null
                  and blocked_username <> ''
                  and blocked_at >= current_timestamp - interval '30 days'
                group by blocked_username
            ),
            video_posts as (
                select username, count(*) as video_count
                from fp_300
                where username is not null
                  and username <> ''
                  and created_at >= current_timestamp - interval '24 hours'
                group by username
            ),
            image_posts as (
                select username, count(*) as image_count
                from fp_400
                where username is not null
                  and username <> ''
                  and created_at >= current_timestamp - interval '24 hours'
                group by username
            ),
            comments as (
                select username, count(*) as comment_count
                from fp_460
                where username is not null
                  and username <> ''
                  and created_at >= current_timestamp - interval '24 hours'
                group by username
            ),
            replies as (
                select username, count(*) as reply_count
                from fp_470
                where username is not null
                  and username <> ''
                  and created_at >= current_timestamp - interval '24 hours'
                group by username
            ),
            usernames as (
                select username from reports
                union select username from blocks
                union select username from video_posts
                union select username from image_posts
                union select username from comments
                union select username from replies
            )
            select
              u.username,
              coalesce(r.report_count, 0) as report_count,
              coalesce(b.blocked_count, 0) as blocked_count,
              coalesce(v.video_count, 0) as video_count,
              coalesce(i.image_count, 0) as image_count,
              coalesce(c.comment_count, 0) as comment_count,
              coalesce(re.reply_count, 0) as reply_count
            from usernames u
            left join reports r on r.username = u.username
            left join blocks b on b.username = u.username
            left join video_posts v on v.username = u.username
            left join image_posts i on i.username = u.username
            left join comments c on c.username = u.username
            left join replies re on re.username = u.username
            where u.username is not null
              and u.username <> ''
            order by
              (coalesce(r.report_count, 0) * 12)
              + (coalesce(b.blocked_count, 0) * 6)
              + (least(coalesce(v.video_count, 0) + coalesce(i.image_count, 0) + coalesce(c.comment_count, 0) + coalesce(re.reply_count, 0), 20) * 2) desc,
              coalesce(r.report_count, 0) desc,
              coalesce(b.blocked_count, 0) desc,
              u.username asc
            limit :limit
            """, params, (rs, rowNum) -> {
            long reportCount = rs.getLong("report_count");
            long blockedCount = rs.getLong("blocked_count");
            long videoCount = rs.getLong("video_count");
            long imageCount = rs.getLong("image_count");
            long commentCount = rs.getLong("comment_count");
            long replyCount = rs.getLong("reply_count");
            long totalActivity = videoCount + imageCount + commentCount + replyCount;
            int score = calculateRiskUserScore(reportCount, blockedCount, totalActivity);
            return new RiskUserItem(
                rs.getString("username"),
                reportCount,
                blockedCount,
                buildRecentActivityLabel(videoCount, imageCount, commentCount, replyCount),
                buildRecommendedAction(score, reportCount, blockedCount, totalActivity),
                score
            );
        });
        return new RiskUserResponse(items);
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Long.class);
        return value != null ? value : 0L;
    }

    private double queryDouble(String sql) {
        Number value = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Number.class);
        return value != null ? value.doubleValue() : 0.0;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private String mapChangedField(String changeTp, String beforeEx, String afterEx) {
        String normalized = normalizeToken(changeTp);
        if ("CD_001".equals(normalized) || normalized.contains("EMAIL")) {
            return "이메일";
        }
        if ("CD_002".equals(normalized) || normalized.contains("PHONE")) {
            return "전화번호";
        }
        if ("CD_003".equals(normalized) || normalized.contains("PASSWORD")) {
            return "비밀번호";
        }
        if ("CD_004".equals(normalized) || containsRole(beforeEx) || containsRole(afterEx) || normalized.contains("ROLE")) {
            return "권한";
        }
        if ("CD_005".equals(normalized) || normalized.contains("REGION")) {
            return "활동 지역";
        }
        if ("CD_006".equals(normalized) || normalized.contains("PROFILE") || normalized.contains("IMAGE")) {
            return "프로필 이미지";
        }
        if ("CD_007".equals(normalized) || normalized.contains("WITHDRAW") || normalized.contains("STATUS")) {
            return "회원 상태";
        }
        if ("CD_008".equals(normalized) || normalized.contains("NICK")) {
            return "닉네임";
        }
        if ("CD_009".equals(normalized) || normalized.contains("PRIVATE") || normalized.contains("PRIVACY")) {
            return "비공개 설정";
        }
        if ("CD_010".equals(normalized) || normalized.contains("SIGNUP") || normalized.contains("REGISTER")) {
            return "회원 상태";
        }
        return fallbackChangedField(beforeEx, afterEx, normalized);
    }

    private String fallbackChangedField(String beforeEx, String afterEx, String normalized) {
        if (containsRole(beforeEx) || containsRole(afterEx)) {
            return "권한";
        }
        if (compact(beforeEx).contains("@") || compact(afterEx).contains("@")) {
            return "이메일";
        }
        return normalized.isBlank() ? "회원 정보" : normalized;
    }
    private boolean containsRole(String value) {
        return normalizeToken(value).contains("ROLE");
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "...";
    }

    private String summarizeUsernames(String usernames) {
        if (usernames == null || usernames.isBlank()) {
            return null;
        }
        String[] parts = usernames.split(",\\s*");
        if (parts.length <= 2) {
            return usernames;
        }
        return parts[0] + ", " + parts[1] + " 외 " + (parts.length - 2) + "명";
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private int calculateRiskUserScore(long reportCount, long blockedCount, long totalActivity) {
        int score = (int) Math.min(100, (reportCount * 12) + (blockedCount * 6) + (Math.min(totalActivity, 20) * 2));
        return Math.max(score, 0);
    }

    private String buildRecentActivityLabel(long videoCount, long imageCount, long commentCount, long replyCount) {
        long totalActivity = videoCount + imageCount + commentCount + replyCount;
        if (totalActivity == 0) {
            return "24시간 내 특이 활동 없음";
        }
        if (imageCount >= videoCount && imageCount >= commentCount && imageCount >= replyCount) {
            return "24시간 내 이미지 피드 " + imageCount + "건";
        }
        if (videoCount >= commentCount && videoCount >= replyCount) {
            return "24시간 내 비디오 업로드 " + videoCount + "건";
        }
        if (commentCount >= replyCount) {
            return "24시간 내 댓글 작성 " + commentCount + "건";
        }
        return "24시간 내 대댓글 작성 " + replyCount + "건";
    }

    private String buildRecommendedAction(int score, long reportCount, long blockedCount, long totalActivity) {
        if (score >= 90 || reportCount >= 6 || blockedCount >= 12) {
            return "즉시 계정 점검";
        }
        if (reportCount >= 3 || blockedCount >= 5) {
            return "콘텐츠 숨김 검토";
        }
        if (totalActivity >= 15) {
            return "도배 여부 확인";
        }
        return "모니터링 유지";
    }

    private record LoginRiskCandidate(
        String username,
        String riskType,
        String riskLabel,
        String detail,
        String ipAddress,
        String deviceId,
        int score,
        LocalDateTime lastOccurredAt
    ) {
    }
}




