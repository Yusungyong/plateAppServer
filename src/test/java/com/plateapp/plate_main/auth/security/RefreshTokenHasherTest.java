package com.plateapp.plate_main.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    @Test
    void sha256DoesNotReturnPlainToken() {
        String token = "refresh-token-value";

        String hashed = RefreshTokenHasher.sha256(token);

        assertThat(hashed).hasSize(64);
        assertThat(hashed).isNotEqualTo(token);
        assertThat(RefreshTokenHasher.sha256(token)).isEqualTo(hashed);
    }
}
