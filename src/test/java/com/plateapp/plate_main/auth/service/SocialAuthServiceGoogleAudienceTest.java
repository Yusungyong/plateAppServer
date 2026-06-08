package com.plateapp.plate_main.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plateapp.plate_main.auth.exception.AuthException;
import com.plateapp.plate_main.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SocialAuthServiceGoogleAudienceTest {

    private static final String IOS_CLIENT_ID = "ios-google-client";
    private static final String ANDROID_CLIENT_ID =
            "398195276422-4hosgi2odjcq47hoqhrk5lc37eo5dcbi.apps.googleusercontent.com";

    @Test
    void verifyGoogleReauthenticationAcceptsExistingClientId() {
        RestTemplate restTemplate = new RestTemplate();
        SocialAuthService service = service(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=ios-token"))
                .andRespond(withSuccess(googleTokenPayload(IOS_CLIENT_ID), MediaType.APPLICATION_JSON));

        String providerUserId = service.verifyGoogleReauthentication("ios-token");

        assertThat(providerUserId).isEqualTo("google-sub");
        server.verify();
    }

    @Test
    void verifyGoogleReauthenticationAcceptsAndroidClientId() {
        RestTemplate restTemplate = new RestTemplate();
        SocialAuthService service = service(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=android-token"))
                .andRespond(withSuccess(googleTokenPayload(ANDROID_CLIENT_ID), MediaType.APPLICATION_JSON));

        String providerUserId = service.verifyGoogleReauthentication("android-token");

        assertThat(providerUserId).isEqualTo("google-sub");
        server.verify();
    }

    @Test
    void verifyGoogleReauthenticationRejectsUnknownAudience() {
        RestTemplate restTemplate = new RestTemplate();
        SocialAuthService service = service(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=unknown-token"))
                .andRespond(withSuccess(googleTokenPayload("unknown-client"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verifyGoogleReauthentication("unknown-token"))
                .isInstanceOfSatisfying(AuthException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_AUDIENCE_MISMATCH));
        server.verify();
    }

    private SocialAuthService service(RestTemplate restTemplate) {
        SocialAuthService service = new SocialAuthService(
                null,
                null,
                null,
                null,
                null,
                null,
                new ObjectMapper(),
                restTemplate,
                null,
                null
        );
        ReflectionTestUtils.setField(service, "googleClientId", IOS_CLIENT_ID);
        ReflectionTestUtils.setField(service, "googleAndroidClientId", ANDROID_CLIENT_ID);
        ReflectionTestUtils.setField(service, "googleAllowedClientIds", "");
        return service;
    }

    private String googleTokenPayload(String audience) {
        return """
                {
                  "iss": "https://accounts.google.com",
                  "aud": "%s",
                  "sub": "google-sub",
                  "email": "user@example.com",
                  "exp": 4102444800
                }
                """.formatted(audience);
    }
}
