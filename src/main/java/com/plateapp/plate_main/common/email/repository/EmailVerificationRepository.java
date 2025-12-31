// src/main/java/com/plateapp/platemain/common/email/repository/EmailVerificationRepository.java
package com.plateapp.plate_main.common.email.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.common.email.entity.EmailVerification;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 최신 기록 1건
    Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email);

    Optional<EmailVerification> findByEmailAndVerificationCode(String email, String verificationCode);
}
