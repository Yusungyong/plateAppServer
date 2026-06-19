package com.plateapp.plate_main.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.exception.AccountConflictException;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    private AccountValidationService service;

    @BeforeEach
    void setUp() {
        service = new AccountValidationService(userRepository);
    }

    @Test
    void usernameAvailabilityUsesTrimmedValue() {
        when(userRepository.existsById("owner01")).thenReturn(false);

        AccountValidationService.ValidationResult result =
                service.validateAvailability("username", "  owner01  ");

        assertThat(result.field()).isEqualTo("username");
        assertThat(result.value()).isEqualTo("owner01");
        assertThat(result.available()).isTrue();
        assertThat(result.message()).isEqualTo("사용 가능한 회원 ID입니다.");
    }

    @Test
    void emailAvailabilityUsesLowerCaseNormalizedValue() {
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        AccountValidationService.ValidationResult result =
                service.validateAvailability("email", " Owner@Example.COM ");

        assertThat(result.value()).isEqualTo("owner@example.com");
        assertThat(result.available()).isFalse();
        assertThat(result.message()).isEqualTo("이미 가입된 이메일입니다.");
    }

    @Test
    void nicknameDuplicateIsIncludedInFinalConflict() {
        AccountValidationService.NormalizedAccount account =
                service.normalizeAccount("owner01", "owner@example.com", "접시사장");
        when(userRepository.existsById("owner01")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("접시사장")).thenReturn(true);

        assertThatThrownBy(() -> service.assertAvailable(account))
                .isInstanceOfSatisfying(AccountConflictException.class, exception ->
                        assertThat(exception.getFieldErrors()).isEqualTo(Map.of(
                                "nickname", "이미 사용 중인 닉네임입니다."
                        )));
    }

    @Test
    void invalidUsernameIsRejectedWithBadRequestError() {
        assertThatThrownBy(() -> service.validateAvailability("username", "한글id"))
                .isInstanceOf(AppException.class)
                .hasMessage("회원 ID는 영문과 숫자로 구성된 4~30자여야 합니다.");
    }

    @Test
    void unknownFieldIsRejected() {
        assertThatThrownBy(() -> service.validateAvailability("phone", "01012345678"))
                .isInstanceOf(AppException.class)
                .hasMessage("field는 username, email, nickname 중 하나여야 합니다.");
    }
}
