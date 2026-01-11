package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.domain.LoginHistory;
import com.plateapp.plate_main.auth.repository.LoginHistoryRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String username,
            String status,
            String failReason,
            String ipAddress,
            String deviceId,
            String deviceModel,
            String os,
            String osVersion,
            String appVersion
    ) {
        try {
            LoginHistory history = LoginHistory.builder()
                    .username(username)
                    .loginDatetime(OffsetDateTime.now())
                    .ipAddress(ipAddress)
                    .loginStatus(status)
                    .failReason(failReason)
                    .deviceId(deviceId)
                    .deviceModel(deviceModel)
                    .os(os)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .createdAt(OffsetDateTime.now())
                    .build();

            loginHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to log login history for {}: {}", username, e.getMessage());
        }
    }
}
