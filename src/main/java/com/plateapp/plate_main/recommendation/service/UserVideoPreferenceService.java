package com.plateapp.plate_main.recommendation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.recommendation.entity.Fp370VideoEvent;
import com.plateapp.plate_main.recommendation.entity.Fp372VideoFeature;
import com.plateapp.plate_main.recommendation.repository.Fp371UserVideoPreferenceRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserVideoPreferenceService {

    public static final String MODEL_VERSION = "v1";

    private final Fp371UserVideoPreferenceRepository preferenceRepository;

    @Transactional
    public void updateFromEvent(Fp370VideoEvent event, Fp300Store store, Fp372VideoFeature feature) {
        if (event == null || store == null || !hasActor(event)) {
            return;
        }

        double weight = eventWeight(event.getEventType());
        if (weight == 0.0) {
            return;
        }

        int positiveDelta = weight > 0 ? 1 : 0;
        int negativeDelta = weight < 0 ? 1 : 0;
        int impressionDelta = "IMPRESSION".equals(event.getEventType()) ? 1 : 0;
        LocalDateTime lastEventAt = event.getServerEventAt() != null ? event.getServerEventAt() : LocalDateTime.now();

        for (Subject subject : subjects(store, feature)) {
            upsert(
                    event,
                    subject.type(),
                    subject.key(),
                    BigDecimal.valueOf(weight),
                    positiveDelta,
                    negativeDelta,
                    impressionDelta,
                    lastEventAt
            );
        }
    }

    private List<Subject> subjects(Fp300Store store, Fp372VideoFeature feature) {
        Map<String, Subject> subjects = new LinkedHashMap<>();
        put(subjects, "PLACE", store.getPlaceId());
        put(subjects, "CREATOR", store.getUsername());
        if (feature != null) {
            put(subjects, "REGION", feature.getRegion1());
            put(subjects, "REGION", feature.getRegion2());
            put(subjects, "DURATION_BUCKET", feature.getDurationBucket());
        }
        return new ArrayList<>(subjects.values());
    }

    private void upsert(
            Fp370VideoEvent event,
            String subjectType,
            String subjectKey,
            BigDecimal scoreDelta,
            int positiveDelta,
            int negativeDelta,
            int impressionDelta,
            LocalDateTime lastEventAt
    ) {
        if (!hasText(subjectKey)) {
            return;
        }

        if (hasText(event.getUsername())) {
            preferenceRepository.upsertUsernamePreference(
                    event.getUserId(),
                    event.getUsername(),
                    false,
                    null,
                    subjectType,
                    subjectKey,
                    scoreDelta,
                    positiveDelta,
                    negativeDelta,
                    impressionDelta,
                    lastEventAt,
                    MODEL_VERSION
            );
            return;
        }

        if (hasText(event.getGuestId())) {
            preferenceRepository.upsertGuestPreference(
                    null,
                    null,
                    true,
                    event.getGuestId(),
                    subjectType,
                    subjectKey,
                    scoreDelta,
                    positiveDelta,
                    negativeDelta,
                    impressionDelta,
                    lastEventAt,
                    MODEL_VERSION
            );
        }
    }

    private double eventWeight(String eventType) {
        if (eventType == null) {
            return 0.0;
        }
        return switch (eventType) {
            case "IMPRESSION" -> 0.05;
            case "CLICK" -> 0.5;
            case "PLAY_START" -> 0.8;
            case "PLAY_PROGRESS" -> 0.25;
            case "PLAY_COMPLETE" -> 2.5;
            case "LIKE" -> 4.0;
            case "COMMENT", "SHARE" -> 3.0;
            case "UNLIKE" -> -1.0;
            case "SKIP" -> -1.0;
            case "HIDE", "NOT_INTERESTED" -> -4.0;
            case "REPORT" -> -10.0;
            default -> 0.0;
        };
    }

    private boolean hasActor(Fp370VideoEvent event) {
        return hasText(event.getUsername()) || hasText(event.getGuestId()) || event.getUserId() != null;
    }

    private void put(Map<String, Subject> subjects, String type, String key) {
        if (hasText(key)) {
            subjects.put(type + ":" + key, new Subject(type, key));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Subject(String type, String key) {
    }
}
