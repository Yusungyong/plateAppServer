package com.plateapp.plate_main.auth.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import com.plateapp.plate_main.auth.dto.AuthUserDto;
import com.plateapp.plate_main.auth.dto.GoogleIdTokenPayload;
import com.plateapp.plate_main.auth.dto.GoogleLoginRequest;
import com.plateapp.plate_main.auth.dto.KakaoLoginRequest;
import com.plateapp.plate_main.auth.dto.KakaoUserResponse;
import com.plateapp.plate_main.auth.dto.TokenResponse;
import com.plateapp.plate_main.auth.repository.LoginHistoryRepository;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.JwtProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialAuthService {

    private static final String APPLE_JWK_URL = "https://appleid.apple.com/auth/keys";
    private static final Duration APPLE_JWK_TTL = Duration.ofHours(6);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final LoginHistoryRepository loginHistoryRepository;

    private volatile AppleJwkSet cachedAppleKeys;
    private volatile Instant appleKeysFetchedAt;

    @Value("${apple.client-id}")
    private String appleClientId;

    @Value("${google.client-id}")
    private String googleClientId;

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
                    .orElseThrow(() -> new IllegalStateException("User not found by user_id."));
        } else {
            user = createUserForApple(payload);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("User id not found after create.");
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

        return new TokenResponse(accessToken, refreshToken, AuthUserDto.from(user));
    }

    private AppleIdTokenPayload parseAndValidateAppleToken(String identityToken) {
        if (identityToken == null || identityToken.isBlank()) {
            throw new IllegalArgumentException("identityToken is empty.");
        }
        if (appleClientId == null || appleClientId.isBlank()) {
            throw new IllegalStateException("apple.client-id is not configured.");
        }

        String kid = extractAppleKid(identityToken);
        PublicKey publicKey = resolveApplePublicKey(kid);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

            String issuer = claims.getIssuer();
            if (!"https://appleid.apple.com".equals(issuer)) {
                throw new IllegalArgumentException("Invalid Apple token issuer.");
            }

            if (!isAudienceMatch(appleClientId, claims.get("aud"))) {
                throw new IllegalArgumentException("aud mismatch. (apple)");
            }

            if (claims.getExpiration() != null
                    && claims.getExpiration().toInstant().isBefore(Instant.now())) {
                throw new IllegalArgumentException("identityToken expired.");
            }

            return objectMapper.convertValue(claims, AppleIdTokenPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("identityToken parse/verify failed.", e);
        }
    }

    private String extractAppleKid(String identityToken) {
        String[] parts = identityToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("identityToken format invalid.");
        }

        String headerJson = new String(
                Base64.getUrlDecoder().decode(parts[0]),
                StandardCharsets.UTF_8
        );

        try {
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
            Object kid = header.get("kid");
            if (kid == null || kid.toString().isBlank()) {
                throw new IllegalArgumentException("Apple token header kid is missing.");
            }
            return kid.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("identityToken header parse failed.", e);
        }
    }

    private PublicKey resolveApplePublicKey(String kid) {
        AppleJwkSet keys = loadAppleKeys();
        if (keys == null || keys.keys == null || keys.keys.isEmpty()) {
            throw new IllegalStateException("Apple public keys not available.");
        }

        for (AppleJwk key : keys.keys) {
            if (kid.equals(key.kid)) {
                return toRsaPublicKey(key);
            }
        }
        throw new IllegalArgumentException("Apple public key not found for kid.");
    }

    private synchronized AppleJwkSet loadAppleKeys() {
        Instant now = Instant.now();
        if (cachedAppleKeys != null && appleKeysFetchedAt != null
                && now.isBefore(appleKeysFetchedAt.plus(APPLE_JWK_TTL))) {
            return cachedAppleKeys;
        }

        AppleJwkSet fetched = restTemplate.getForObject(APPLE_JWK_URL, AppleJwkSet.class);
        cachedAppleKeys = fetched;
        appleKeysFetchedAt = now;
        return fetched;
    }

    private PublicKey toRsaPublicKey(AppleJwk key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.n);
            byte[] eBytes = Base64.getUrlDecoder().decode(key.e);
            BigInteger modulus = new BigInteger(1, nBytes);
            BigInteger exponent = new BigInteger(1, eBytes);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Apple public key.", e);
        }
    }

    private boolean isAudienceMatch(String expected, Object aud) {
        if (aud instanceof String) {
            return expected.equals(aud);
        }
        if (aud instanceof Collection<?> values) {
            return values.stream().anyMatch(v -> expected.equals(String.valueOf(v)));
        }
        return expected.equals(String.valueOf(aud));
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

    public TokenResponse loginWithKakao(KakaoLoginRequest request) {
        if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("accessToken is empty.");
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
                    .orElseThrow(() -> new IllegalStateException("User not found by user_id."));
        } else {
            user = createUserForKakao(kakaoUser);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("User id not found after create.");
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

        return new TokenResponse(accessToken, refreshToken, AuthUserDto.from(user));
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
            throw new IllegalArgumentException("Kakao token verify failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Kakao token verify failed.", e);
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

    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new IllegalArgumentException("Google idToken is empty.");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("google.client-id is not configured.");
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
                    .orElseThrow(() -> new IllegalStateException("User not found by user_id."));
        } else {
            user = createUserForGoogle(payload);

            Integer userId = userRepository.findUserIdByUsername(user.getUsername());
            if (userId == null) {
                throw new IllegalStateException("User id not found after create.");
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

        return new TokenResponse(accessToken, refreshToken, AuthUserDto.from(user));
    }

    private GoogleIdTokenPayload parseAndValidateGoogleToken(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

            var response = restTemplate.getForEntity(url, GoogleIdTokenPayload.class);
            GoogleIdTokenPayload payload = response.getBody();

            if (payload == null) {
                throw new IllegalArgumentException("Google token payload empty.");
            }

            if (!"accounts.google.com".equals(payload.getIss())
                    && !"https://accounts.google.com".equals(payload.getIss())) {
                throw new IllegalArgumentException("Invalid Google token issuer.");
            }

            if (!googleClientId.equals(payload.getAud())) {
                throw new IllegalArgumentException("aud mismatch. (google)");
            }

            long now = Instant.now().getEpochSecond();
            if (payload.getExp() != null && payload.getExp() < now) {
                throw new IllegalArgumentException("Google idToken expired.");
            }

            return payload;
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("Google token verify failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Google token parse/verify failed.", e);
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

    private static class AppleJwkSet {
        public List<AppleJwk> keys;
    }

    private static class AppleJwk {
        public String kid;
        public String n;
        public String e;
    }
}
