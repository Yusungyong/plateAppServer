package com.plateapp.plate_main.common.email.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailVerifyControllerTest {

    @Test
    void masksEmailBeforeItIsWrittenToLogs() {
        assertThat(EmailVerifyController.maskEmail("alice@example.com")).isEqualTo("a***@example.com");
        assertThat(EmailVerifyController.maskEmail("invalid-address")).isEqualTo("***");
        assertThat(EmailVerifyController.maskEmail("alice@example.com\nforged-log-entry")).isEqualTo("***");
        assertThat(EmailVerifyController.maskEmail("a@" + "x".repeat(253))).isEqualTo("***");
        assertThat(EmailVerifyController.maskEmail(null)).isEqualTo("***");
    }
}
