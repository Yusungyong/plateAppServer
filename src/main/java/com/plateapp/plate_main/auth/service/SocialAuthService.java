// src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java
package com.plateapp.plate_main.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plateapp.plate_main.auth.domain.SocialAccount;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.dto.AppleIdTokenPayload;
import com.plateapp.plate_main.auth.dto.AppleLoginRequest;
import com.plateapp.plate_main.auth.dto.GoogleIdTokenPayload;
import com.plateapp.plate_main.auth.dto.GoogleLoginRequest;
import com.plateapp.plate_main.auth.dto.KakaoLoginRequest;
import com.plateapp.plate_main.auth.dto.KakaoUserResponse;
import com.plateapp.plate_main.auth.dto.TokenResponse;
import com.plateapp.plate_main.auth.repository.LoginHistoryRepository;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialAuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final LoginHistoryRepository loginHistoryRepository;

    /** Apple aud 검증용 (bundle id 또는 services id) */
    @Value("${apple.client-id}")
    private String appleClientId;

    /** Google aud 검증용 (iOS용 client-id) */
    @Value("${google.client-id}")
    private String googleClientId;

    // =======================
    // 🔹 Apple 로그인
    // =======================
    public TokenResponse loginWithApple(AppleLoginRequest request) {

        AppleIdTokenPayload payload = parseAndValidateAppleToken(request.getIdentityToken());

        String provider = "APPLE";
        String providerUserId = payload.getSub();

        Optional<SocialAccount> socialOpt =
                socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user;

        if (socialOpt.isPresent()) {
            Integer userId = socialOpt.get().getUserId();
            user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("소셜 매핑은 있는데 유저가 없습니다."));
        } else {
            user = createUserForApple(payload);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("새 유저 생성 후 user_id 를 찾을 수 없습니다.");
            }

            SocialAccount social = SocialAccount.builder()
                    .userId(userId)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(payload.getEmail())
                    .displayName(user.getNickname())
                    .build();

            socialAccountRepository.save(social);
        }

        String accessToken = jwtProvider.createAccessToken(user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        return new TokenResponse(accessToken, refreshToken, user);
    }

    private AppleIdTokenPayload parseAndValidateAppleToken(String identityToken) {

        if (identityToken == null || identityToken.isBlank()) {
            throw new IllegalArgumentException("identityToken 이 비어 있습니다.");
        }

        String[] parts = identityToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("identityToken 형식이 올바르지 않습니다.");
        }

        String payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
        );

        try {
            AppleIdTokenPayload payload =
                    objectMapper.readValue(payloadJson, AppleIdTokenPayload.class);

            if (!"https://appleid.apple.com".equals(payload.getIss())) {
                throw new IllegalArgumentException("Apple 토큰이 아닙니다.(iss)");
            }

            if (!appleClientId.equals(payload.getAud())) {
                throw new IllegalArgumentException("aud 가 일치하지 않습니다. (apple)");
            }

            long now = Instant.now().getEpochSecond();
            if (payload.getExp() != null && payload.getExp() < now) {
                throw new IllegalArgumentException("identityToken 이 만료되었습니다.");
            }

            return payload;
        } catch (Exception e) {
            throw new IllegalArgumentException("identityToken 파싱에 실패했습니다.", e);
        }
    }

    private User createUserForApple(AppleIdTokenPayload payload) {

        String base = "apple_" + payload.getSub();
        String username = makeUniqueUsername(base, "apple_");

        String encodedPw = passwordEncoder.encode("APPLE-" + UUID.randomUUID());

        return userRepository.save(
                User.builder()
                        .username(username)
                        .password(encodedPw)
                        .email(payload.getEmail())
                        .role("USR")
                        .createdAt(LocalDate.now())
                        .updatedAt(LocalDate.now())
                        .isPrivate(false)
                        .build()
        );
    }

    // =======================
    // 🔹 Kakao 로그인
    // =======================
    public TokenResponse loginWithKakao(KakaoLoginRequest request) {

        if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("카카오 accessToken 이 비어 있습니다.");
        }

        KakaoUserResponse kakaoUser = getKakaoUserInfo(request.getAccessToken());

        String provider = "KAKAO";
        String providerUserId = String.valueOf(kakaoUser.getId());

        var socialOpt =
                socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user;

        if (socialOpt.isPresent()) {
            Integer userId = socialOpt.get().getUserId();
            user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("소셜 매핑은 있는데 유저가 없습니다."));
        } else {
            user = createUserForKakao(kakaoUser);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("새 유저 생성 후 user_id 를 찾을 수 없습니다.");
            }

            String email = kakaoUser.getKakaoAccount() != null
                    ? kakaoUser.getKakaoAccount().getEmail()
                    : null;
            String nickname = (kakaoUser.getKakaoAccount() != null &&
                    kakaoUser.getKakaoAccount().getProfile() != null)
                    ? kakaoUser.getKakaoAccount().getProfile().getNickname()
                    : null;

            SocialAccount social = SocialAccount.builder()
                    .userId(userId)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(email)
                    .displayName(nickname)
                    .build();

            socialAccountRepository.save(social);
        }

        String accessToken = jwtProvider.createAccessToken(user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        return new TokenResponse(accessToken, refreshToken, user);
    }

    private KakaoUserResponse getKakaoUserInfo(String accessToken) {
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);

            var entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    KakaoUserResponse.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("카카오 토큰 검증 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회 실패", e);
        }
    }

    private User createUserForKakao(KakaoUserResponse kakaoUser) {
        String base = "kakao_" + kakaoUser.getId();
        String username = makeUniqueUsername(base, "kakao_");

        String encodedPw = passwordEncoder.encode("KAKAO-" + UUID.randomUUID());

        String email = kakaoUser.getKakaoAccount() != null
                ? kakaoUser.getKakaoAccount().getEmail()
                : null;
        String nickname = (kakaoUser.getKakaoAccount() != null &&
                kakaoUser.getKakaoAccount().getProfile() != null)
                ? kakaoUser.getKakaoAccount().getProfile().getNickname()
                : null;

        return userRepository.save(
                User.builder()
                        .username(username)
                        .password(encodedPw)
                        .email(email)
                        .nickname(nickname)
                        .role("USR")
                        .createdAt(LocalDate.now())
                        .updatedAt(LocalDate.now())
                        .isPrivate(false)
                        .build()
        );
    }

    // =======================
    // 🔹 Google 로그인
    // =======================
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {

        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new IllegalArgumentException("Google idToken 이 비어 있습니다.");
        }

        GoogleIdTokenPayload payload = parseAndValidateGoogleToken(request.getIdToken());

        String provider = "GOOGLE";
        String providerUserId = payload.getSub();

        var socialOpt =
                socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user;

        if (socialOpt.isPresent()) {
            Integer userId = socialOpt.get().getUserId();
            user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("소셜 매핑은 있는데 유저가 없습니다."));
        } else {
            user = createUserForGoogle(payload);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("새 유저 생성 후 user_id 를 찾을 수 없습니다.");
            }

            String email = payload.getEmail();
            String name = payload.getName();

            SocialAccount social = SocialAccount.builder()
                    .userId(userId)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(email)
                    .displayName(name)
                    .build();

            socialAccountRepository.save(social);
        }

        String accessToken = jwtProvider.createAccessToken(user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        return new TokenResponse(accessToken, refreshToken, user);
    }

    private GoogleIdTokenPayload parseAndValidateGoogleToken(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

            var response = restTemplate.getForEntity(url, GoogleIdTokenPayload.class);
            GoogleIdTokenPayload payload = response.getBody();

            if (payload == null) {
                throw new IllegalArgumentException("Google 토큰 정보가 비어 있습니다.");
            }

            if (!"accounts.google.com".equals(payload.getIss())
                    && !"https://accounts.google.com".equals(payload.getIss())) {
                throw new IllegalArgumentException("Google 토큰이 아닙니다.(iss)");
            }

            if (!googleClientId.equals(payload.getAud())) {
                throw new IllegalArgumentException("aud 가 일치하지 않습니다. (google)");
            }

            long now = Instant.now().getEpochSecond();
            if (payload.getExp() != null && payload.getExp() < now) {
                throw new IllegalArgumentException("Google idToken 이 만료되었습니다.");
            }

            return payload;
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("Google 토큰 검증 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Google 토큰 파싱/검증 실패", e);
        }
    }

    private User createUserForGoogle(GoogleIdTokenPayload payload) {
        String base = "google_" + payload.getSub();
        String username = makeUniqueUsername(base, "google_");

        String encodedPw = passwordEncoder.encode("GOOGLE-" + UUID.randomUUID());

        return userRepository.save(
                User.builder()
                        .username(username)
                        .password(encodedPw)
                        .email(payload.getEmail())
                        .nickname(payload.getName())
                        .role("USR")
                        .createdAt(LocalDate.now())
                        .updatedAt(LocalDate.now())
                        .isPrivate(false)
                        .build()
        );
    }


    // =======================
    // 🔹 공통 유틸: username 중복 방지
    // =======================
    private String makeUniqueUsername(String base, String prefix) {
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsById(candidate)) {
            String s = "_" + suffix++;
            int maxBaseLen = 20 - s.length();
            String trimmedBase = base.length() > maxBaseLen
                    ? base.substring(0, maxBaseLen)
                    : base;
            candidate = trimmedBase + s;
        }
        return candidate;
    }
}
