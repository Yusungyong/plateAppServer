package com.plateapp.plate_main.notification.repository;

import com.plateapp.plate_main.notification.entity.Fp24UserPushToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Fp24UserPushTokenRepository extends JpaRepository<Fp24UserPushToken, Long> {
    List<Fp24UserPushToken> findByUserIdAndTokenStatusOrderByUpdatedAtDesc(Integer userId, String tokenStatus);
    Optional<Fp24UserPushToken> findByUserIdAndDeviceId(Integer userId, String deviceId);
    Optional<Fp24UserPushToken> findByPushToken(String pushToken);
}
