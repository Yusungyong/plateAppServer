package com.plateapp.plate_main.owner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.service.AccountValidationService;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.security.RateLimitService;
import com.plateapp.plate_main.owner.dto.OwnerAccountValidationDtos;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class OwnerAccountValidationControllerTest {

    @Mock
    private AccountValidationService accountValidationService;
    @Mock
    private RateLimitService rateLimitService;

    @Test
    void validateAppliesIpRateLimitAndReturnsFieldResult() {
        OwnerAccountValidationController controller =
                new OwnerAccountValidationController(accountValidationService, rateLimitService);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
        when(rateLimitService.clientIp(httpRequest)).thenReturn("127.0.0.1");
        when(accountValidationService.validateAvailability("username", "owner01"))
                .thenReturn(new AccountValidationService.ValidationResult(
                        "username",
                        "owner01",
                        true,
                        "사용 가능한 회원 ID입니다."
                ));

        ApiResponse<OwnerAccountValidationDtos.Response> response = controller.validate(
                new OwnerAccountValidationDtos.Request("username", "owner01"),
                httpRequest
        );

        verify(rateLimitService).check(
                "owner:signup-account-validation:127.0.0.1",
                60,
                Duration.ofMinutes(1)
        );
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().available()).isTrue();
        assertThat(response.getData().value()).isEqualTo("owner01");
    }
}
