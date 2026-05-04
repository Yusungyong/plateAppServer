package com.plateapp.plate_main.auth.service.impl;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.domain.UserHistory;
import com.plateapp.plate_main.auth.repository.UserHistoryRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.service.PasswordResetService;
import com.plateapp.plate_main.common.email.service.EmailVerifyService;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final EmailVerifyService emailVerifyService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            UserHistoryRepository userHistoryRepository,
            EmailVerifyService emailVerifyService
    ) {
        this.userRepository = userRepository;
        this.userHistoryRepository = userHistoryRepository;
        this.emailVerifyService = emailVerifyService;
    }

    @Override
    public void resetPassword(String email, String verificationCode, String newPassword) {
        if (!emailVerifyService.isVerifiedCodeValid(email, verificationCode)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid or unverified email verification code.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        userRepository.save(user);

        UserHistory history = UserHistory.builder()
                .username(user.getUsername())
                .beforeEx("{\"passwordChanged\":false}")
                .afterEx("{\"passwordChanged\":true,\"resetMethod\":\"email_verification\"}")
                .changeTp("CD_003")
                .createdDt(LocalDateTime.now())
                .build();

        userHistoryRepository.save(history);
    }
}
