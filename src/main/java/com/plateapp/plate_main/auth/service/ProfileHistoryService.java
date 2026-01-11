package com.plateapp.plate_main.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plateapp.plate_main.auth.domain.UserHistory;
import com.plateapp.plate_main.auth.dto.ProfileHistoryRequest;
import com.plateapp.plate_main.auth.dto.ProfileHistoryResponse;
import com.plateapp.plate_main.auth.repository.UserHistoryRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileHistoryService {

    private final UserHistoryRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProfileHistoryResponse record(String username, ProfileHistoryRequest request) {
        String beforeJson = toJsonSafe(request.getBefore());
        String afterJson = toJsonSafe(mergeAfterAndMemo(request.getAfter(), request.getMemo()));

        UserHistory history = UserHistory.builder()
                .username(username)
                .beforeEx(beforeJson)
                .afterEx(afterJson)
                .changeTp(request.getChangeType())
                .createdDt(LocalDateTime.now())
                .build();

        UserHistory saved = repository.save(history);

        return ProfileHistoryResponse.builder()
                .historyId(saved.getHistoryId())
                .loggedAt(saved.getCreatedDt())
                .build();
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize profile history payload: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> mergeAfterAndMemo(Map<String, Object> after, String memo) {
        if (memo == null || memo.isBlank()) {
            return after;
        }
        Map<String, Object> result = new HashMap<>();
        if (after != null) {
            result.putAll(after);
        }
        result.put("memo", memo);
        return result;
    }
}
