package com.plateapp.plate_main.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.domain.SocialAccount;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.api.ApiResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private Authentication authentication;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userRepository, socialAccountRepository);
    }

    @Test
    void meIncludesSocialProviderForSocialAccount() {
        User user = User.builder()
                .username("user@example.com")
                .email("user@example.com")
                .nickname("tester")
                .profileImageUrl("https://example.com/profile.png")
                .role("USR")
                .userId(10)
                .build();
        SocialAccount socialAccount = SocialAccount.builder()
                .userId(10)
                .provider("KAKAO")
                .providerUserId("12345")
                .build();

        when(authentication.getPrincipal()).thenReturn("user@example.com");
        when(userRepository.findById("user@example.com")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findFirstByUserIdOrderByCreatedAtDesc(10))
                .thenReturn(Optional.of(socialAccount));

        ApiResponse<UserController.MeRes> response = controller.me(authentication);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().username()).isEqualTo("user@example.com");
        assertThat(response.getData().nickname()).isEqualTo("tester");
        assertThat(response.getData().social()).isNotNull();
        assertThat(response.getData().social().provider()).isEqualTo("kakao");
    }

    @Test
    void meOmitsSocialForPasswordAccount() {
        User user = User.builder()
                .username("user123")
                .email("user@example.com")
                .nickname("tester")
                .role("USR")
                .userId(11)
                .build();

        when(authentication.getPrincipal()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findFirstByUserIdOrderByCreatedAtDesc(11))
                .thenReturn(Optional.empty());

        ApiResponse<UserController.MeRes> response = controller.me(authentication);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().username()).isEqualTo("user123");
        assertThat(response.getData().social()).isNull();
    }
}
