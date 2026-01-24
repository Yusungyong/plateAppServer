// src/main/java/com/plateapp/plate_main/auth/repository/SocialAccountRepository.java
package com.plateapp.plate_main.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.auth.domain.SocialAccount;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<SocialAccount> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);
}
