// src/main/java/com/plateapp/platemain/common/email/service/impl/EmailVerifyServiceImpl.java
package com.plateapp.plate_main.common.email.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.email.dto.EmailVerifyVO;
import com.plateapp.plate_main.common.email.entity.EmailVerification;
import com.plateapp.plate_main.common.email.repository.EmailVerificationRepository;
import com.plateapp.plate_main.common.email.service.EmailVerifyService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerifyServiceImpl implements EmailVerifyService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@plate-service.com}")
    private String fromEmail;

    // 6자리 인증 코드 생성
    private String generateVerificationCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    @Override
    @Transactional
    public void sendVerificationEmail(EmailVerifyVO emailVerifyVO) {
        // 이메일 존재 여부 확인 (username + email 기준, 혹은 email만)
        Optional<User> userOpt;
        if (emailVerifyVO.getUsername() != null) {
            userOpt = userRepository.findByUsernameAndEmail(
                    emailVerifyVO.getUsername(),
                    emailVerifyVO.getEmail()
            );
        } else {
            userOpt = userRepository.findByEmail(emailVerifyVO.getEmail());
        }

        // 유저가 없으면: 아무것도 안 하고 리턴 (컨트롤러에서는 항상 "전송되었습니다" 응답)
        if (userOpt.isEmpty()) {
            return;
        }

        String email = emailVerifyVO.getEmail();
        String code = generateVerificationCode();

        // ✅ 메일 보내기 (실패해도 예외 안 터지게 위에서 처리)
        sendVerificationMail(email, code);

        // ✅ DB에 코드 저장/업데이트는 메일 실패 여부와 상관없이 진행
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByIdDesc(email)
                .orElseGet(() -> {
                    EmailVerification ev = new EmailVerification();
                    ev.setEmail(email);
                    return ev;
                });

        verification.setVerificationCode(code);
        verification.setIsVerified(false);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        emailVerificationRepository.save(verification);

        System.out.println("[INFO] 이메일 인증코드 생성 및 저장 완료. email=" + email + ", code=" + code);
    }


    private void sendVerificationMail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[접시] 이메일 인증 코드");
            helper.setText(
                    "<h2>이메일 인증 코드</h2>" +
                            "<p>아래 코드를 입력하세요:</p>" +
                            "<h3>" + code + "</h3>" +
                            "<p>이 코드는 10분간 유효합니다.</p>",
                    true
            );

            mailSender.send(message);
        } catch (Exception e) {
            // 🔴 개발 단계에서는 그냥 로그만 찍고 진행 (클라이언트에 500 안 던지도록)
            System.err.println("[WARN] 이메일 전송 실패: " + e.getMessage());
            // e.printStackTrace(); // 필요하면 자세히
        }
    }


    @Override
    @Transactional
    public boolean verifyEmail(EmailVerifyVO emailVerifyVO) {
        String email = emailVerifyVO.getEmail();
        String inputCode = emailVerifyVO.getVerificationCode();

        Optional<EmailVerification> opt = emailVerificationRepository.findTopByEmailOrderByIdDesc(email);

        if (opt.isEmpty()) {
            return false;
        }

        EmailVerification verification = opt.get();

        // 코드/만료 체크
        if (!verification.getVerificationCode().equals(inputCode) || (verification.getExpiresAt() != null &&
                verification.getExpiresAt().isBefore(LocalDateTime.now()))) {
            return false;
        }

        // 인증 성공 처리
        verification.setIsVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        return true;
    }

    @Override
    public String findUsernameByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUsername)
                .orElse(null);
    }

    @Override
    public void sendUsernameEmail(String to, String username) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[접시] 아이디 안내");
        helper.setText(
                "<p>접시 어플리케이션에 등록된 아이디는 다음과 같습니다:</p>" +
                        "<h3>" + username + "</h3>",
                true
        );

        mailSender.send(message);
    }

    // 필요하면 비밀번호 재설정 코드용 메일도 재사용 가능
    public void sendPasswordResetCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[접시] 비밀번호 재설정 코드");
            helper.setText(
                    "<h2>비밀번호 재설정 코드</h2>" +
                            "<p>아래 코드를 입력하세요:</p>" +
                            "<h3>" + code + "</h3>" +
                            "<p>이 코드는 5분간 유효합니다.</p>",
                    true
            );

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("비밀번호 재설정 코드 발송 중 오류가 발생했습니다.", e);
        }
    }
}
