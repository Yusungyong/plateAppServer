# Android Social Login Server Check Guide

Last updated: 2026-05-26

## 목적

이 문서는 Android 소셜 로그인 이슈를 서버 관점에서 빠르게 분류하기 위한 점검 문서입니다.

핵심은 아래 두 가지입니다.

1. Android 요청이 실제로 서버까지 들어오는지
2. 서버에 들어온 뒤 iOS와 동일 기준으로 정상 검증되는지

---

## 서버 현재 처리 방식

서버는 Android와 iOS를 별도로 분기하지 않습니다.

- Kakao
  - `POST /api/auth/social/kakao`
  - 입력값: `accessToken`
- Google
  - `POST /api/auth/social/google`
  - 입력값: `idToken`

즉 서버 기준으로는 플랫폼 구분 없이 **provider token 기준 검증**입니다.

관련 파일:

- `src/main/java/com/plateapp/plate_main/auth/controller/AuthController.java`
- `src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java`

---

## Android 대상 범위

Android에서 서버 확인 대상은 아래 두 개입니다.

- Kakao
- Google

Apple은 프론트에서 Android에 노출하지 않으므로 서버 점검 대상이 아닙니다.

---

## 서버 응답 계약

Android와 iOS 모두 서버 응답 shape는 동일합니다.

### 기존 회원

```json
{
  "success": true,
  "data": {
    "kind": "login_success",
    "accessToken": "...",
    "refreshToken": "..."
  }
}
```

### 신규 회원

```json
{
  "success": true,
  "data": {
    "kind": "signup_required",
    "signupToken": "...",
    "provider": "google",
    "providerUserId": "...",
    "email": "...",
    "nickname": "..."
  }
}
```

관련 파일:

- `src/main/java/com/plateapp/plate_main/auth/dto/SocialAuthResponse.java`

---

## Android 이슈 분류 기준

### 1. 서버 요청 로그 자체가 없다

이 경우는 서버 문제가 아니라 **프론트/SDK 단계에서 막힌 것**으로 봐야 합니다.

대표 예:

- Android Google에서 `GOOGLE_WEB_CLIENT_ID` 미설정
- Android Google에서 `idToken` 획득 실패
- Android Kakao에서 SDK callback 또는 app scheme 문제

즉 서버에서는 추가 조치할 것이 거의 없습니다.

### 2. 서버 요청은 들어오지만 로그인 실패

이 경우는 서버 token 검증 또는 계정 매핑 문제 가능성이 있습니다.

대표 예:

- Kakao access token 무효
- Google idToken 무효
- Google `aud` mismatch
- provider user 식별은 됐지만 계정 연결/조회 실패

---

## Android Kakao 서버 점검 포인트

서버 Kakao 로직은 단순합니다.

1. `accessToken` 존재 확인
2. `https://kapi.kakao.com/v2/user/me` 호출
3. provider user id 조회
4. 기존 계정이면 `login_success`
5. 신규 계정이면 `signup_required`

즉 Android Kakao에서 서버가 볼 핵심은 아래입니다.

- `/api/auth/social/kakao` 요청이 실제로 들어오는지
- Kakao user info API 호출이 성공하는지
- 기존 계정인지 신규 계정인지 정상 분기되는지

관련 파일:

- `src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java`

---

## Android Google 서버 점검 포인트

서버 Google 로직은 `idToken`의 `aud`를 엄격하게 검사합니다.

검사 순서:

1. `idToken` 존재 확인
2. `https://oauth2.googleapis.com/tokeninfo?id_token=...` 호출
3. issuer 확인
4. `aud == google.client-id` 확인
5. 만료 확인
6. 기존 계정이면 `login_success`
7. 신규 계정이면 `signup_required`

여기서 가장 중요한 점:

- 서버는 `payload.aud`를 `google.client-id=${GOOGLE_CLIENT_ID}`와 **정확히 비교**합니다.
- Android에서 웹 클라이언트 ID가 아닌 다른 client ID로 `idToken`을 받으면 서버는 거부합니다.

즉 Android Google 이슈에서 가장 먼저 확인할 항목은:

1. Android가 실제로 서버까지 요청을 보내는지
2. Android가 받은 `idToken`의 `aud`
3. 서버 `GOOGLE_CLIENT_ID`

이 셋이 맞는지입니다.

관련 파일:

- `src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java`
- `src/main/resources/application.yaml`

---

## 서버 설정 확인 포인트

### Google

서버는 아래 설정을 사용합니다.

```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID}
```

따라서 운영/스테이징/local 환경에서 `GOOGLE_CLIENT_ID` 값이 올바른지 확인이 필요합니다.

### Apple

Android 대상은 아니지만 서버에는 아래 설정도 존재합니다.

```yaml
apple:
  client-id: ${APPLE_CLIENT_ID}
```

---

## requestId 추적

서버는 `ApiResponse`에 `requestId`를 포함하고, 로그에도 같은 `requestId`를 남깁니다.

따라서 Android 실패 케이스를 추적할 때는 아래 순서가 가장 효율적입니다.

1. 프론트가 받은 응답의 `requestId` 확보
2. 서버 로그에서 같은 `requestId` 검색
3. 요청 유입 여부 및 실패 지점 확인

관련 파일:

- `src/main/java/com/plateapp/plate_main/common/api/ApiResponse.java`
- `src/main/java/com/plateapp/plate_main/common/filter/RequestIdFilter.java`
- `src/main/resources/application.yaml`

---

## 현재 서버 관점 판단

### 정상인 부분

- Android/iOS 응답 계약은 동일
- Kakao/Google provider별 엔드포인트 존재
- `login_success / signup_required` 분기 존재
- requestId 기반 추적 가능

### 가장 가능성 높은 Android 이슈

#### Google

- Android에서 서버 요청 전 단계 실패
- 또는 서버 도달 후 `aud mismatch`

#### Kakao

- 서버 로그 유입이 없으면 Android SDK/callback 단계 문제
- 서버 로그 유입이 있으면 Kakao token verify 실패 여부 확인

---

## 서버 측 주의사항

현재 소셜 token 검증 실패는 내부적으로 `IllegalArgumentException`을 사용하고 있습니다.

이 경우 전역 예외 처리에서 최종적으로 일반 내부 오류처럼 보일 수 있어, 프론트가 원인을 구분하기 어렵습니다.

따라서 장기적으로는 아래 개선을 권장합니다.

- 소셜 token 검증 실패를 `AppException` 또는 `AuthException`으로 변환
- `400` 또는 `401` 계열 `ApiResponse.fail(errorCode, message)`로 명확히 응답

즉 현재 Android 점검은 가능하지만, 에러 표현 품질은 더 개선할 여지가 있습니다.

---

## 서버 점검 체크리스트

1. Android Kakao 실패 시 `/api/auth/social/kakao` 요청 로그가 실제로 있는지 확인
2. Android Google 실패 시 `/api/auth/social/google` 요청 로그가 실제로 있는지 확인
3. Android Google 요청이 있다면 `idToken` 검증 실패 사유가 `aud mismatch`인지 확인
4. 서버 환경변수 `GOOGLE_CLIENT_ID` 값 확인
5. 프론트가 받은 `requestId`로 서버 로그 역추적
6. 기존 회원이면 `login_success`, 신규 회원이면 `signup_required`가 동일하게 내려가는지 확인

---

## 한 줄 결론

Android 소셜 로그인 이슈는 서버가 플랫폼별로 다르게 처리해서 생길 가능성보다, **서버 도달 전 프론트/SDK 문제** 또는 **Android Google의 `aud` 불일치 문제**일 가능성이 더 큽니다.
