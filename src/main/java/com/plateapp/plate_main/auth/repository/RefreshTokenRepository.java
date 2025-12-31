// src/main/java/com/plateapp/plate_main/auth/repository/RefreshTokenRepository.java
package com.plateapp.plate_main.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByRefreshToken(String token);

    void deleteByUsername(String username);

    void deleteByUsernameAndDeviceId(String username, String deviceId);
}
