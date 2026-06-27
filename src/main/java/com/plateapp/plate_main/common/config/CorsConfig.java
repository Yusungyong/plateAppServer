package com.plateapp.plate_main.common.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.plateapp.plate_main.auth.security.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {
    private final CorsProperties properties;
    private static final List<String> ALLOWED_METHODS = List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    );
    private static final List<String> ALLOWED_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.AUTHORIZATION,
            "content-type",
            "authorization",
            "X-Requested-With",
            "X-Request-Id"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS.toArray(String[]::new))
                .allowedHeaders(ALLOWED_HEADERS.toArray(String[]::new))
                .exposedHeaders("X-Request-Id")
                .allowCredentials(properties.allowCredentials())
                .maxAge(properties.maxAge().toSeconds());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(properties.allowCredentials());
        configuration.setMaxAge(properties.maxAge().toSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
