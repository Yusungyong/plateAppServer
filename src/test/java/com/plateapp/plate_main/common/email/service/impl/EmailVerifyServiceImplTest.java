package com.plateapp.plate_main.common.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.email.repository.EmailVerificationRepository;

class EmailVerifyServiceImplTest {

    @Test
    void verificationCodesUseOneReusableSecureRandomAndKeepSixDigitFormat() {
        EmailVerifyServiceImpl service = new EmailVerifyServiceImpl(
                mock(EmailVerificationRepository.class),
                mock(UserRepository.class),
                mock(JavaMailSender.class)
        );

        Object firstGenerator = ReflectionTestUtils.getField(EmailVerifyServiceImpl.class, "SECURE_RANDOM");
        Object secondGenerator = ReflectionTestUtils.getField(EmailVerifyServiceImpl.class, "SECURE_RANDOM");
        assertThat(firstGenerator).isInstanceOf(SecureRandom.class).isSameAs(secondGenerator);

        for (int i = 0; i < 1_000; i++) {
            String code = ReflectionTestUtils.invokeMethod(service, "generateVerificationCode");
            assertThat(code).matches("[1-9][0-9]{5}");
        }
    }
}
