package com.plateapp.plate_main.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void checkRejectsRequestsAfterLimit() {
        RateLimitService rateLimitService = new RateLimitService();

        rateLimitService.check("auth:login:127.0.0.1:user", 2, Duration.ofMinutes(1));
        rateLimitService.check("auth:login:127.0.0.1:user", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() ->
                rateLimitService.check("auth:login:127.0.0.1:user", 2, Duration.ofMinutes(1))
        ).isInstanceOf(RateLimitException.class);
    }
}
