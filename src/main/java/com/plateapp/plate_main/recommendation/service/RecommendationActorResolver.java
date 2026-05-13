package com.plateapp.plate_main.recommendation.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.plateapp.plate_main.user.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecommendationActorResolver {

    private final MemberRepository memberRepository;

    public RecommendationActor resolve(
            Authentication authentication,
            String usernameParam,
            Boolean isGuest,
            String guestId,
            String sessionId
    ) {
        String authenticatedUsername = currentUsername(authentication);
        if (hasText(authenticatedUsername)) {
            Integer userId = memberRepository.findById(authenticatedUsername)
                    .map(user -> user.getUserId())
                    .orElse(null);
            return new RecommendationActor(userId, authenticatedUsername, false, null, sessionId);
        }

        if (Boolean.TRUE.equals(isGuest) && hasText(guestId)) {
            return new RecommendationActor(null, null, true, guestId, sessionId);
        }

        if (hasText(usernameParam)) {
            Integer userId = memberRepository.findById(usernameParam)
                    .map(user -> user.getUserId())
                    .orElse(null);
            return new RecommendationActor(userId, usernameParam, false, null, sessionId);
        }

        return RecommendationActor.none();
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return null;
        }
        String principal = String.valueOf(authentication.getPrincipal());
        return "anonymousUser".equals(principal) ? null : principal;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
