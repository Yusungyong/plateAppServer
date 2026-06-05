// src/main/java/com/plateapp/plate_main/auth/security/SecurityConfig.java
package com.plateapp.plate_main.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Swagger/OpenAPI (springdoc)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html"
                    // Add more paths here if needed for your environment
                    // "/webjars/**"
                ).permitAll()

                // Public endpoints
                .requestMatchers(
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/social/**",
                    "/api/email/**",
                    "/api/auth/reset-password",
                    "/api/health",
                    "/api/home/random-candidates/**",
                    "/api/map/stores/**",
                    "/files/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/home/video-thumbnails",
                    "/api/home/image-thumbnails",
                    "/api/home/content-feed",
                    "/api/home/feed",
                    "/api/home/seasonal",
                    "/api/home/seasonal/*",
                    "/api/faqs",
                    "/api/faqs/*",
                    "/api/qna",
                    "/api/qna/*",
                    "/api/search",
                    "/api/search/suggest",
                    "/api/image-feeds/*",
                    "/api/image-feeds/context",
                    "/api/image-feeds/groups",
                    "/api/image-feeds/groups/*/images"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/qna").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/home/video-events").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/home/impressions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/detail/*/public-profile").authenticated()

                .requestMatchers("/api/admin/member-monitoring/**").hasAnyAuthority(
                    PlateAuthorities.AUTHORITY_ADMIN,
                    PlateAuthorities.PERMISSION_ADMIN_ACCESS,
                    PlateAuthorities.PERMISSION_MEMBER_MONITORING_READ
                )
                .requestMatchers("/api/admin/restaurants", "/api/admin/restaurants/**", "/api/admin/files").hasAnyAuthority(
                    PlateAuthorities.AUTHORITY_ADMIN,
                    PlateAuthorities.PERMISSION_ADMIN_ACCESS,
                    PlateAuthorities.PERMISSION_RESTAURANT_MANAGE
                )
                .requestMatchers("/api/admin/**").hasAnyAuthority(
                    PlateAuthorities.AUTHORITY_ADMIN,
                    PlateAuthorities.PERMISSION_ADMIN_ACCESS
                )
                .requestMatchers("/api/users/detail/**").hasAnyAuthority(
                    PlateAuthorities.AUTHORITY_ADMIN,
                    PlateAuthorities.PERMISSION_ADMIN_ACCESS
                )
                .requestMatchers(HttpMethod.POST, "/api/users/*/profile-history").hasAnyAuthority(
                    PlateAuthorities.AUTHORITY_ADMIN,
                    PlateAuthorities.PERMISSION_ADMIN_ACCESS
                )

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


