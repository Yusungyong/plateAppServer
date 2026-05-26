# 프로필/계정 API 응답 표준화 확인 요청

작성일: 2026-05-26  
대상: 프론트엔드 프로필/계정 API 연동

## 배경

서버 API 응답 포맷을 `common.api.ApiResponse` 기준으로 점진 통일 중입니다.

현재 대부분의 성공 응답은 아래 형태로 정리하고 있습니다.

```json
{
  "success": true,
  "data": {},
  "message": null,
  "errorCode": null,
  "requestId": "...",
  "timestamp": "..."
}
```

## 확인이 필요한 API

아래 프로필/계정 관련 API는 아직 기존 응답 구조를 유지하고 있습니다.

- `PUT /api/users/me/password`
- `DELETE /api/users/me`
- `DELETE /api/users/me/social`

## 현재 실패 응답 예시

비밀번호 변경 API는 현재 아래처럼 내려갈 수 있습니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_PASSWORD",
    "message": "..."
  }
}
```

계정 삭제 API는 별도 응답 형태를 사용합니다.

```json
{
  "success": false,
  "message": "...",
  "errorCode": "INVALID_PASSWORD"
}
```

## 서버 표준화 예정 응답

서버에서는 위 API들도 아래 표준 형태로 통일하려고 합니다.

```json
{
  "success": false,
  "data": null,
  "message": "...",
  "errorCode": "INVALID_PASSWORD",
  "requestId": "...",
  "timestamp": "..."
}
```

## 프론트 확인 요청

프론트에서 위 3개 API의 오류 처리 시 아래 필드를 직접 참조하고 있는지 확인 부탁드립니다.

- `error.code`
- `error.message`
- 계정 삭제 API의 기존 `message`
- 계정 삭제 API의 기존 `errorCode`

직접 참조 중이라면 서버 응답 표준화 시 프론트 파싱 로직도 함께 수정이 필요합니다.

## 권장 프론트 처리 방향

전환 후에는 오류 코드는 `errorCode`, 사용자 표시 문구는 `message`를 기준으로 처리하면 됩니다.

```ts
const code = response.errorCode;
const message = response.message;
```

`requestId`는 장애 문의나 로그 추적용으로 사용할 수 있습니다.
