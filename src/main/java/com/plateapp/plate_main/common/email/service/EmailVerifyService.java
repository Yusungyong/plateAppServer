// src/main/java/com/plateapp/platemain/common/email/service/EmailVerifyService.java
package com.plateapp.plate_main.common.email.service;

import com.plateapp.plate_main.common.email.dto.EmailVerifyVO;

public interface EmailVerifyService {

    // 이메일 인증 코드 발송 (회원 존재 확인 포함)
    void sendVerificationEmail(EmailVerifyVO emailVerifyVO);

    // 인증 코드 검증
    boolean verifyEmail(EmailVerifyVO emailVerifyVO);

    // 이메일로 아이디 찾기
    String findUsernameByEmail(String email);

    // 찾은 아이디를 이메일로 발송
    void sendUsernameEmail(String to, String username) throws Exception;
}
