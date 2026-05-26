# ApiResponse Migration Plan

작성일: 2026-05-26  
대상: Plate Service API 응답 포맷 정리

## 1. 현재 결론

서버에는 `ApiResponse`가 2종 존재했다.

| 구분 | 패키지 | 형태 | 상태 |
|---|---|---|---|
| 표준 후보 | `common.api.ApiResponse` | `success`, `data`, `message`, `errorCode`, `requestId`, `timestamp` | 유지 |
| 이전 대상 | `common.dto.ApiResponse` | `success`, `data`, `error { code, message }` | 단계적 제거 |

신규 관리자 식당 API, 인증 API, 공통 예외 처리, 댓글/피드/영상 일부 API는 이미 `common.api.ApiResponse`를 사용한다.  
이번 작업에서는 프론트 영향이 낮은 단순 성공 응답 Controller를 먼저 표준 응답으로 전환했다.

## 2. 이번에 전환한 Controller

아래 Controller는 `common.dto.ApiResponse`에서 `common.api.ApiResponse`로 전환했다.

| Controller | 주요 API | 전환 판단 |
|---|---|---|
| `StoreLikeController` | `/api/stores/{storeId}/likes/*` | 성공 응답만 wrapper 사용 |
| `FriendManagementController` | `/api/friends/manage/**` | 성공 응답만 wrapper 사용 |
| `ProfileDetailController` | `/api/users/{username}/profile-detail`, `/api/my/profile-detail` | 성공 응답만 wrapper 사용 |
| `ProfileActivitySummaryController` | 활동 요약 | 성공 응답만 wrapper 사용 |
| `ProfileActivityDetailController` | 활동 상세, 좋아요 목록, 좋아요 맵 | 성공 응답만 wrapper 사용 |

성공 응답의 `data` 구조는 유지된다. 다만 wrapper에 `requestId`, `timestamp`가 추가될 수 있다.

## 3. 아직 남은 이전 응답 사용처

현재 `common.dto.ApiResponse`는 아래 한 곳에만 남아 있다.

| Controller | 남긴 이유 |
---|---|
| `ProfileController` | 비밀번호 변경 API에서 `ApiResponse.error(...)`를 직접 반환하고, 계정 삭제 API는 별도 `DeleteAccountResponse`를 사용한다. 프론트 오류 처리 영향이 있어 별도 협의 후 전환 권장 |

관련 API:

- `GET /api/users/me`
- `GET /api/users/{username}`
- `PUT /api/users/me`
- `POST /api/users/me/push-token`
- `POST /api/users/me/profile-image`
- `DELETE /api/users/me/profile-image`
- `PUT /api/users/me/password`
- `GET /api/users/me/stats`
- `GET /api/users/{username}/stats`
- `DELETE /api/users/me`
- `DELETE /api/users/me/social`

## 4. 다음 전환 제안

### 1단계: ProfileController 성공 응답 전환

성공 응답만 먼저 `common.api.ApiResponse.ok(...)`로 바꾼다.

대상:

- 프로필 조회/수정
- push token 동기화
- 프로필 이미지 업로드/삭제
- stats 조회

영향:

- `data`는 유지된다.
- wrapper에 `requestId`, `timestamp`가 추가된다.
- 프론트가 `success`, `data`만 본다면 영향이 작다.

### 2단계: 비밀번호 변경 오류 응답 전환

현재:

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

전환 후:

```json
{
  "success": false,
  "message": "...",
  "errorCode": "INVALID_PASSWORD",
  "requestId": "...",
  "timestamp": "..."
}
```

프론트가 `error.code`, `error.message`를 직접 읽고 있다면 수정이 필요하다.

### 3단계: 계정 삭제 응답 전환

현재 `DELETE /api/users/me`, `DELETE /api/users/me/social`은 `DeleteAccountResponse`를 직접 반환한다.

현재:

```json
{
  "success": true,
  "message": "Account deleted successfully",
  "errorCode": null
}
```

전환 후보:

```json
{
  "success": true,
  "data": {
    "message": "Account deleted successfully"
  },
  "requestId": "...",
  "timestamp": "..."
}
```

또는 성공 시 `ApiResponse.ok(null, "Account deleted successfully")`로 통일할 수 있다. 이 부분은 프론트 UX와 파싱 방식을 확인한 뒤 결정하는 편이 안전하다.

## 5. 프론트 공유 문장

서버 응답 포맷을 `common.api.ApiResponse` 기준으로 점진 통일 중입니다.  
이번 단계에서는 좋아요, 친구, 프로필 상세/활동 API의 성공 응답 wrapper가 표준 포맷으로 변경됩니다. `data` 내부 구조는 유지되며, wrapper에 `requestId`, `timestamp`가 추가될 수 있습니다.  
아직 `ProfileController`의 비밀번호 변경/계정 삭제 응답은 기존 구조를 유지하고 있으며, 해당 API는 프론트 오류 처리 방식 확인 후 별도 전환 예정입니다.

## 6. 완료 조건

- `common.dto.ApiResponse` import 사용처 0개
- `DeleteAccountResponse` 직접 반환 제거 또는 사용 이유 문서화
- 공통 예외 응답과 Controller 직접 오류 응답 구조 통일
- API 문서의 성공/실패 응답 예시를 `common.api.ApiResponse` 기준으로 갱신
