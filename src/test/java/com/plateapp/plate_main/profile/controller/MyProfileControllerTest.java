package com.plateapp.plate_main.profile.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.profile.dto.MyProfileRequest;
import com.plateapp.plate_main.profile.dto.MyProfileResponse;
import com.plateapp.plate_main.profile.dto.MyProfileResponse.Settings;
import com.plateapp.plate_main.profile.dto.MyProfileResponse.Stats;
import com.plateapp.plate_main.profile.service.MyProfileService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class MyProfileControllerTest {

    @Mock
    private MyProfileService myProfileService;

    @Test
    void getProfileUsesAuthenticatedUsernameInsteadOfRequestUsername() {
        MyProfileController controller = new MyProfileController(myProfileService);
        MyProfileResponse response = new MyProfileResponse(
                1,
                "me",
                "me@example.com",
                "Me",
                null,
                LocalDate.of(2026, 6, 5),
                new Stats(0, 0, 0, 0, 0),
                new Settings(true, false, "ko")
        );
        when(myProfileService.getProfile("me", true)).thenReturn(response);

        controller.getProfile(new MyProfileRequest("other-user", true), auth("me"));

        verify(myProfileService).getProfile("me", true);
    }

    private Authentication auth(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, null);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
