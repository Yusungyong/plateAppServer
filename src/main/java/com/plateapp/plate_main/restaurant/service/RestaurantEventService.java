package com.plateapp.plate_main.restaurant.service;

import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.restaurant.dto.RestaurantEventDtos;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantEventService {

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of(
            "DETAIL_VIEW",
            "MAP_IMPRESSION",
            "SEARCH_IMPRESSION",
            "PHONE_CLICK",
            "DIRECTION_CLICK",
            "SHARE_CLICK",
            "MENU_VIEW",
            "MENU_SAVE",
            "VISIT_CONVERSION",
            "REVIEW_CONVERSION"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMenuRepository restaurantMenuRepository;
    private final UserRepository userRepository;

    @Transactional
    public RestaurantEventDtos.EventRecordResponse record(
            Long restaurantId,
            RestaurantEventDtos.EventRecordRequest request,
            String username,
            String userAgent
    ) {
        if (restaurantId == null || !restaurantRepository.existsById(restaurantId)) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND);
        }
        if (request == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "event body is required.");
        }

        String eventType = normalizeEventType(request.eventType());
        Long menuId = request.menuId();
        if (menuId != null && !restaurantMenuRepository.existsByIdAndRestaurantId(menuId, restaurantId)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "menuId does not belong to restaurant.");
        }

        String safeUsername = blankToNull(username);
        Integer userId = safeUsername == null ? null : userRepository.findUserIdByUsername(safeUsername);
        boolean isGuest = Boolean.TRUE.equals(request.isGuest()) || safeUsername == null;
        String eventUid = truncate(blankToNull(request.eventUid()), 120);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("restaurantId", restaurantId)
                .addValue("eventType", eventType)
                .addValue("eventUid", eventUid)
                .addValue("username", truncate(safeUsername, 255))
                .addValue("userId", userId)
                .addValue("isGuest", isGuest)
                .addValue("guestId", truncate(blankToNull(request.guestId()), 120))
                .addValue("sessionId", truncate(blankToNull(request.sessionId()), 150))
                .addValue("deviceId", truncate(blankToNull(request.deviceId()), 200))
                .addValue("surface", truncate(blankToNull(request.surface()), 60))
                .addValue("source", truncate(blankToNull(request.source()), 60))
                .addValue("menuId", menuId)
                .addValue("contentType", normalizeContentType(request.contentType()))
                .addValue("contentId", request.contentId())
                .addValue("clientEventAt", request.clientEventAt())
                .addValue("serverEventAt", OffsetDateTime.now(ZoneOffset.UTC))
                .addValue("userAgent", truncate(blankToNull(userAgent), 300));

        try {
            List<Long> ids = jdbc.queryForList("""
                    insert into restaurant_analytics_events (
                        restaurant_id,
                        event_type,
                        event_uid,
                        username,
                        user_id,
                        is_guest,
                        guest_id,
                        session_id,
                        device_id,
                        surface,
                        event_source,
                        menu_id,
                        content_type,
                        content_id,
                        client_event_at,
                        server_event_at,
                        user_agent
                    ) values (
                        :restaurantId,
                        :eventType,
                        :eventUid,
                        :username,
                        :userId,
                        :isGuest,
                        :guestId,
                        :sessionId,
                        :deviceId,
                        :surface,
                        :source,
                        :menuId,
                        :contentType,
                        :contentId,
                        :clientEventAt,
                        :serverEventAt,
                        :userAgent
                    )
                    on conflict (event_uid) where event_uid is not null do nothing
                    returning id
                    """, params, Long.class);

            return new RestaurantEventDtos.EventRecordResponse(
                    ids.isEmpty() ? null : ids.get(0),
                    !ids.isEmpty(),
                    eventType
            );
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid restaurant event.");
        }
    }

    private String normalizeEventType(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "eventType is required.");
        }
        normalized = normalized.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_EVENT_TYPES.contains(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported eventType.");
        }
        return normalized;
    }

    private String normalizeContentType(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return truncate(normalized.trim().toLowerCase(Locale.ROOT), 20);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
