package com.plateapp.plate_main.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class HttpClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HttpClientConfig.class);

    @Test
    void appliesConservativeDefaultTimeouts() {
        contextRunner.run(context -> assertTimeouts(context.getBean(RestTemplate.class), 5_000, 10_000));
    }

    @Test
    void allowsTimeoutsToBeConfiguredWithDurationProperties() {
        contextRunner
                .withPropertyValues(
                        "app.http-client.connect-timeout-millis=750",
                        "app.http-client.read-timeout-millis=3000"
                )
                .run(context -> assertTimeouts(context.getBean(RestTemplate.class), 750, 3_000));
    }

    private void assertTimeouts(RestTemplate restTemplate, int connectTimeout, int readTimeout) {
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
        Object requestFactory = restTemplate.getRequestFactory();
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(connectTimeout);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(readTimeout);
    }
}
