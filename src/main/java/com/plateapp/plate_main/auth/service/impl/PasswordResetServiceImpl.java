// src/main/java/com/plateapp/plate_main/auth/service/impl/PasswordResetServiceImpl.java
package com.plateapp.plate_main.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.domain.UserHistory;
import com.plateapp.plate_main.auth.repository.UserHistoryRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.service.PasswordResetService;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    UserHistoryRepository userHistoryRepository) {
        this.userRepository = userRepository;
        this.userHistoryRepository = userHistoryRepository;
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 유저가 없습니다."));

        // 기존 비밀번호
        String before = user.getPassword();

        // 새 비밀번호 암호화
        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);

        // 비밀번호 업데이트
        userRepository.save(user);

        // 🔥 변경 이력 저장
        UserHistory history = UserHistory.builder()
                .username(user.getUsername())
                .beforeEx(before)
                .afterEx(encoded)
                .changeTp("CD_003")  // 비밀번호 변경
                .createdDt(LocalDateTime.now())
                .build();

        userHistoryRepository.save(history);
    }
}
