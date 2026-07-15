// src/main/java/com/plateapp/plate_main/config/HttpClientConfig.java
package com.plateapp.plate_main.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Value("${app.http-client.connect-timeout-millis:5000}")
    private int connectTimeoutMillis = 5_000;

    @Value("${app.http-client.read-timeout-millis:10000}")
    private int readTimeoutMillis = 10_000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        return new RestTemplate(requestFactory);
    }
}
