package com.plateapp.plate_main.mypage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import com.plateapp.plate_main.mypage.service.MyHubService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MyHubControllerTest {

    @Mock
    private MyHubService myHubService;

    @Test
    void usesAuthenticatedPrincipalAndDefaultPreviewLimit() {
        MyHubResponse response = emptyResponse();
        when(myHubService.getHub("me", 3)).thenReturn(response);
        MyHubController controller = new MyHubController(myHubService);

        ApiResponse<MyHubResponse> result = controller.getHub(null, auth("me"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        verify(myHubService).getHub("me", 3);
    }

    @Test
    void acceptsBothZeroAndMaximumPreviewLimit() {
        MyHubResponse response = emptyResponse();
        when(myHubService.getHub("me", 0)).thenReturn(response);
        when(myHubService.getHub("me", 6)).thenReturn(response);
        MyHubController controller = new MyHubController(myHubService);

        controller.getHub("0", auth("me"));
        controller.getHub("6", auth("me"));

        verify(myHubService).getHub("me", 0);
        verify(myHubService).getHub("me", 6);
    }

    @Test
    void invalidPreviewLimitUsesFlatCommon400AppException() {
        MyHubController controller = new MyHubController(myHubService);

        assertInvalidPreviewLimit(controller, "abc");
        assertInvalidPreviewLimit(controller, "");
        assertInvalidPreviewLimit(controller, "   ");
        assertInvalidPreviewLimit(controller, "-1");
        assertInvalidPreviewLimit(controller, "7");
    }

    @Test
    void rejectsMissingAuthentication() {
        MyHubController controller = new MyHubController(myHubService);

        assertThatThrownBy(() -> controller.getHub("1", null))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));
    }

    private void assertInvalidPreviewLimit(MyHubController controller, String value) {
        assertThatThrownBy(() -> controller.getHub(value, auth("me")))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
                    assertThat(exception.getMessage()).isEqualTo("previewLimit는 0 이상 6 이하여야 합니다.");
                });
    }

    private TestingAuthenticationToken auth(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, null);
        authentication.setAuthenticated(true);
        return authentication;
    }

    private MyHubResponse emptyResponse() {
        return new MyHubResponse(
                new MyHubResponse.Profile("me", "me", null, null, false),
                new MyHubResponse.Counts(0, 0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                MyHubResponse.PrimaryAction.EXPLORE_CONTENT,
                Instant.parse("2026-07-16T09:00:00Z")
        );
    }
}
