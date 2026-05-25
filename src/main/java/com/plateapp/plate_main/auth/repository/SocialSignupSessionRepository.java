package com.plateapp.plate_main.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.auth.domain.SocialSignupSession;

public interface SocialSignupSessionRepository extends JpaRepository<SocialSignupSession, Long> {

    Optional<SocialSignupSession> findBySignupToken(String signupToken);
}
