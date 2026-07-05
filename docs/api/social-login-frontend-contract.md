# 소셜 로그인 프론트엔드 연동 계약

이 문서는 React Native 프론트엔드에서 Kakao, Google, Apple 소셜 로그인을 연동할 때 사용할 서버 API 계약입니다. 서버 구현 기준으로 작성했습니다.

## 결론

- Android Kakao/Google 연동은 현재 서버 API로 진행 가능합니다.
- iOS Apple/Kakao/Google도 서버 엔드포인트는 준비되어 있습니다.
- Naver 앱 소셜 로그인 API는 없습니다.
- 필드명은 `nickname`을 표준으로 사용합니다. `nickName`은 사용하지 않습니다.
- `GET /api/me`는 소셜 계정인 경우 `social.provider`를 내려줍니다.

## Android 확인 현황

서버에서 확인한 항목:

- `POST /api/auth/social/kakao`는 Android SDK가 발급한 Kakao `accessToken`을 받습니다.
- `POST /api/auth/social/google`은 Android SDK가 발급한 Google `idToken`을 받습니다.
- `Google ID token` 검증 시 `GOOGLE_ANDROID_CLIENT_ID` audience를 허용합니다.
- Android 요청 payload의 `deviceId`, `deviceModel`, `os`, `osVersion`, `appVersion`, `fcmToken` 필드를 수신합니다.
- `/api/auth/social/**` 경로는 인증 없이 호출 가능합니다.
- 소셜 로그인 이후 `GET /api/me`에서 `social.provider`를 확인할 수 있습니다.

로컬 확인 결과:

- 현재 서버 워크스페이스와 로컬 `C:\workspace` 기준으로 React Native Android 프로젝트 파일은 발견되지 않았습니다.
- 따라서 `android/app/google-services.json`, Android package name, SHA 인증서 해시, Kakao native app key, Kakao manifest scheme은 이 저장소에서 직접 검증할 수 없습니다.

프론트 Android 프로젝트에서 확인해야 할 파일:

- `android/app/google-services.json`
- `android/app/build.gradle`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/values/strings.xml`
- `src/screens/auth/LoginScreen.tsx`
- `src/auth/AuthProvider.tsx`
- `src/auth/socialAuth.ts`

## 공통 응답

성공 응답은 `ApiResponse` 래퍼를 사용합니다.

```json
{
  "success": true,
  "data": {}
}
```

실패 응답은 `message`, `errorCode`를 포함합니다.

```json
{
  "success": false,
  "message": "소셜 로그인 토큰을 검증하지 못했습니다.",
  "errorCode": "AUTH_SOCIAL_TOKEN_INVALID"
}
```

## 소셜 로그인

### Kakao

```http
POST /api/auth/social/kakao
```

Request:

```json
{
  "accessToken": "kakao_access_token",
  "deviceId": "device_unique_id_or_null",
  "deviceModel": "Pixel 8",
  "os": "Android",
  "osVersion": "16",
  "appVersion": "2.0.5",
  "fcmToken": "optional_fcm_token"
}
```

### Google

```http
POST /api/auth/social/google
```

Request:

```json
{
  "idToken": "google_id_token",
  "deviceId": "device_unique_id_or_null",
  "deviceModel": "Pixel 8",
  "os": "Android",
  "osVersion": "16",
  "appVersion": "2.0.5",
  "fcmToken": "optional_fcm_token"
}
```

### Apple

```http
POST /api/auth/social/apple
```

Request:

```json
{
  "identityToken": "apple_identity_token",
  "authorizationCode": "optional_authorization_code",
  "user": "optional_raw_user_json",
  "deviceId": "device_unique_id_or_null",
  "deviceModel": "iPhone",
  "os": "iOS",
  "osVersion": "18",
  "appVersion": "2.0.5",
  "fcmToken": "optional_fcm_token"
}
```

## 기존 회원 로그인 성공

```json
{
  "success": true,
  "data": {
    "kind": "login_success",
    "accessToken": "app_access_token",
    "refreshToken": "app_refresh_token",
    "user": {
      "username": "user@example.com",
      "email": "user@example.com",
      "nickname": "사용자닉네임",
      "profileImageUrl": "https://...",
      "activeRegion": "서울"
    }
  }
}
```

프론트 처리:

- `data.kind === "login_success"`이면 `accessToken`, `refreshToken`을 저장합니다.
- 이후 `GET /api/me`를 호출해 현재 사용자 상태를 동기화합니다.

## 추가 회원가입 필요

소셜 provider 검증은 성공했지만 앱 계정이 아직 없으면 아래 응답을 반환합니다.

```json
{
  "success": true,
  "data": {
    "kind": "signup_required",
    "signupToken": "temporary_signup_token",
    "provider": "kakao",
    "providerUserId": "provider_user_id",
    "email": "user@example.com",
    "nickname": "사용자닉네임"
  }
}
```

프론트 처리:

- `data.kind === "signup_required"`이면 소셜 회원가입 화면으로 이동합니다.
- `signupToken`은 회원가입 완료 API에 그대로 전달합니다.
- `email`, `nickname`은 입력 초기값으로 사용할 수 있습니다.

## 소셜 회원가입 완료

```http
POST /api/auth/social/signup/complete
```

Request:

```json
{
  "signupToken": "temporary_signup_token",
  "email": "user@example.com",
  "nickname": "사용자닉네임",
  "agreeService": true,
  "agreePrivacy": true
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "app_access_token",
    "refreshToken": "app_refresh_token",
    "user": {
      "username": "user@example.com",
      "email": "user@example.com",
      "nickname": "사용자닉네임",
      "profileImageUrl": null,
      "activeRegion": null
    }
  }
}
```

서버 정책:

- `signupToken`은 30분 후 만료됩니다.
- 이미 사용된 `signupToken`은 다시 사용할 수 없습니다.
- `agreeService`, `agreePrivacy`는 반드시 `true`여야 합니다.
- 같은 이메일이 이미 사용 중이면 자동 연결하지 않고 충돌로 처리합니다.

## 현재 사용자 조회

소셜 로그인 또는 소셜 회원가입 완료 후 호출합니다.

```http
GET /api/me
Authorization: Bearer {accessToken}
```

소셜 계정 응답:

```json
{
  "success": true,
  "data": {
    "username": "user@example.com",
    "nickname": "사용자닉네임",
    "email": "user@example.com",
    "profileImageUrl": "https://...",
    "role": "USR",
    "social": {
      "provider": "kakao"
    }
  }
}
```

일반 계정 응답:

```json
{
  "success": true,
  "data": {
    "username": "user123",
    "nickname": "사용자닉네임",
    "email": "user@example.com",
    "profileImageUrl": "https://...",
    "role": "USR"
  }
}
```

주의:

- `nickname`을 사용합니다. `nickName`은 서버 표준 필드가 아닙니다.
- `social.provider`는 소셜 계정일 때만 내려갑니다.
- provider 값은 소문자 `kakao`, `google`, `apple` 형식입니다.

## 토큰 갱신

```http
POST /api/auth/refresh
```

Request:

```json
{
  "refreshToken": "app_refresh_token"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "new_app_access_token",
    "refreshToken": "new_app_refresh_token"
  }
}
```

refresh가 401 또는 403으로 실패하면 프론트는 저장된 토큰을 삭제하고 로그아웃 상태로 전환하면 됩니다.

## 서버 설정 참고

Google ID token audience는 아래 서버 환경변수 중 하나와 일치해야 합니다.

- `GOOGLE_CLIENT_ID`
- `GOOGLE_ANDROID_CLIENT_ID`
- `GOOGLE_ALLOWED_CLIENT_IDS`

Apple identity token audience는 `APPLE_CLIENT_ID`와 일치해야 합니다.

Kakao는 프론트에서 받은 `accessToken`으로 Kakao 사용자 정보 API를 호출해 검증합니다.

## 운영 진단 참고

소셜 로그인 실패는 `fp_105.fail_reason`에 provider와 서버 error code를 함께 기록합니다.

예시:

- `GOOGLE_LOGIN_FAILED:AUTH_SOCIAL_AUDIENCE_MISMATCH`
- `GOOGLE_LOGIN_FAILED:AUTH_SOCIAL_TOKEN_INVALID`
- `GOOGLE_LOGIN_FAILED:AUTH_SOCIAL_PROVIDER_USER_NOT_FOUND`

Android Google 로그인이 실패하면 먼저 아래 쿼리로 최신 실패 사유와 디바이스 정보를 확인합니다.

```sql
select login_datetime, username, login_status, fail_reason,
       os, os_version, app_version, device_model, device_id, ip_address
from fp_105
where fail_reason like 'GOOGLE_LOGIN_FAILED%'
order by login_datetime desc
limit 50;
```

서버 로그에는 Google `idToken` 원문을 남기지 않습니다. 대신 audience mismatch일 때 `aud`와 서버 허용 client id 목록을 마스킹해 남깁니다.

```bash
sudo journalctl -u plate-main.service -n 500 --no-pager | grep -Ei 'Google social token|GOOGLE_LOGIN_FAILED|AUTH_SOCIAL'
```

`GOOGLE_LOGIN_FAILED:AUTH_SOCIAL_AUDIENCE_MISMATCH`이면 Android 앱에서 발급된 Google ID token의 `aud`가 서버의 `GOOGLE_CLIENT_ID`, `GOOGLE_ANDROID_CLIENT_ID`, `GOOGLE_ALLOWED_CLIENT_IDS` 중 어디에도 포함되지 않은 상태입니다. 이 경우 프론트의 Google Web Client ID 또는 Android Client ID를 서버 환경변수에 추가해야 합니다.
