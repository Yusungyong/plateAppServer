// src/main/java/com/plateapp/platemain/common/email/controller/EmailVerifyController.java
package com.plateapp.plate_main.common.email.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.email.dto.EmailVerifyVO;
import com.plateapp.plate_main.common.email.service.EmailVerifyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/email") // prefix 살짝 명시
@RequiredArgsConstructor
public class EmailVerifyController {

    private final EmailVerifyService emailVerifyService;
    private static final Logger logger = LoggerFactory.getLogger(EmailVerifyController.class);

    /**
     * 이메일로 인증 코드 발송
     */
    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody EmailVerifyVO emailVerifyVO) {
        try {
            emailVerifyService.sendVerificationEmail(emailVerifyVO);
            // 유저가 없더라도 동일 응답 (보안상 계정 존재 여부 노출 방지)
            logger.info("이메일 {} 로 인증 코드 발송 요청 처리 완료", emailVerifyVO.getEmail());
            return ResponseEntity.ok(
                    Map.of("success", true, "message", "인증 코드가 이메일로 전송되었습니다.")
            );
        } catch (Exception e) {
            logger.error("인증 코드 발송 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "이메일 발송 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사용자가 입력한 인증 코드 검증
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerifyVO emailVerifyVO) {
        try {
            boolean ok = emailVerifyService.verifyEmail(emailVerifyVO);

            if (ok) {
                logger.info("이메일 {} 인증 성공", emailVerifyVO.getEmail());
                return ResponseEntity.ok(
                        Map.of("success", true, "message", "이메일 인증 성공!")
                );
            } else {
                logger.warn("이메일 {} 인증 실패 (입력 코드: {})",
                        emailVerifyVO.getEmail(), emailVerifyVO.getVerificationCode());
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "인증 코드가 올바르지 않거나 만료되었습니다."));
            }
        } catch (Exception e) {
            logger.error("이메일 인증 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "이메일 인증 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 아이디 찾기
     */
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = emailVerifyService.findUsernameByEmail(email);

        if (username != null) {
            try {
                emailVerifyService.sendUsernameEmail(email, username);
                return ResponseEntity.ok(
                        Map.of("success", true, "message", "가입된 아이디를 이메일로 발송했습니다.")
                );
            } catch (Exception e) {
                logger.error("아이디 안내 메일 발송 실패: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "메일 발송 실패"));
            }
        } else {
            return ResponseEntity.ok(
                    Map.of("success", false, "message", "일치하는 계정을 찾을 수 없습니다.")
            );
        }
    }
}
