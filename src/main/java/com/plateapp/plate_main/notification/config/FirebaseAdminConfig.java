package com.plateapp.plate_main.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class FirebaseAdminConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(loadCredentials());

        if (StringUtils.hasText(firebaseProperties.getProjectId())) {
            builder.setProjectId(firebaseProperties.getProjectId());
        }

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(builder.build(), "plate-main");
        log.info("FirebaseApp initialized. projectId={}", firebaseProperties.getProjectId());
        return firebaseApp;
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(firebaseProperties.getServiceAccountPath())) {
            try (InputStream inputStream = new FileInputStream(firebaseProperties.getServiceAccountPath())) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
