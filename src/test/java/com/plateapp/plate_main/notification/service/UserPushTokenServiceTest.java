package com.plateapp.plate_main.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.notification.entity.Fp24UserPushToken;
import com.plateapp.plate_main.notification.repository.Fp24UserPushTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPushTokenServiceTest {

    @Mock
    private Fp24UserPushTokenRepository tokenRepository;

    private UserPushTokenService service;

    @BeforeEach
    void setUp() {
        service = new UserPushTokenService(tokenRepository);
    }

    @Test
    void upsertAppTokenUpdatesExistingRowWhenPushTokenAlreadyExists() {
        User user = User.builder()
                .username("user47")
                .userId(47)
                .build();
        Fp24UserPushToken existing = new Fp24UserPushToken();
        existing.setUserId(12);
        existing.setDeviceId("old-device");
        existing.setPlatform("ANDROID");
        existing.setPushToken("same-token");
        existing.setTokenStatus("ACTIVE");

        when(tokenRepository.findByPushToken("same-token")).thenReturn(Optional.of(existing));
        when(tokenRepository.save(existing)).thenReturn(existing);

        service.upsertAppToken(user, "new-device", "ios", "same-token");

        verify(tokenRepository, never()).findByUserIdAndDeviceId(47, "new-device");
        ArgumentCaptor<Fp24UserPushToken> captor = ArgumentCaptor.forClass(Fp24UserPushToken.class);
        verify(tokenRepository).save(captor.capture());

        Fp24UserPushToken saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getUserId()).isEqualTo(47);
        assertThat(saved.getDeviceId()).isEqualTo("new-device");
        assertThat(saved.getPlatform()).isEqualTo("IOS");
        assertThat(saved.getPushToken()).isEqualTo("same-token");
        assertThat(saved.getTokenStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getLastSeenAt()).isNotNull();
    }

    @Test
    void upsertAppTokenFallsBackToDeviceLookupWhenPushTokenIsNew() {
        User user = User.builder()
                .username("user47")
                .userId(47)
                .build();
        Fp24UserPushToken existingDevice = new Fp24UserPushToken();
        existingDevice.setUserId(47);
        existingDevice.setDeviceId("device-1");

        when(tokenRepository.findByPushToken("new-token")).thenReturn(Optional.empty());
        when(tokenRepository.findByUserIdAndDeviceId(47, "device-1")).thenReturn(Optional.of(existingDevice));
        when(tokenRepository.save(existingDevice)).thenReturn(existingDevice);

        service.upsertAppToken(user, "device-1", "APP", "new-token");

        verify(tokenRepository).findByUserIdAndDeviceId(47, "device-1");
        assertThat(existingDevice.getPushToken()).isEqualTo("new-token");
        assertThat(existingDevice.getTokenStatus()).isEqualTo("ACTIVE");
    }
}
