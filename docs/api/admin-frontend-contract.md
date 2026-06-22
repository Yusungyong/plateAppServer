# Plate 관리자 프론트 연동 계약

기준일: 2026-06-22

## 기존 앱 호환성

- 기존 앱 API의 성공·실패 응답 형식과 인증 갱신 동작은 변경하지 않는다.
- 관리자 전용 매장 운영 상태와 피드 추천 상태, 낙관적 잠금 버전은 별도 관리자 테이블에 저장한다.
- 관리자 피드 숨김·복원은 기존 `fp_400.use_yn`만 동기화하므로 앱에는 공개 여부만 반영된다.
- 관리자 매장 노출 변경은 기존 매장 공개 상태에 반영되지만, 운영 상태·사유·버전은 앱 모델에 추가하지 않는다.

## 공통 계약

- API base URL은 환경별 배포 URL의 `/api`이다.
- 성공 응답은 `{ "success": true, "data": ... }` 형식이다.
- 실패 응답은 기존 `ApiResponse` 형식을 유지한다.

```json
{
  "success": false,
  "data": null,
  "message": "요청을 처리할 수 없습니다.",
  "errorCode": "COMMON_400",
  "requestId": "...",
  "timestamp": "2026-06-22T00:00:00Z"
}
```

입력값 검증 오류의 필드별 상세 내용은 `data`에 포함된다. 목록 응답의 `data`는 `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`를 가진다. 관리자 변경 요청의 `version`이 현재 값과 다르면 HTTP 409 `COMMON_409`를 반환한다.

OpenAPI는 `SPRINGDOC_API_DOCS_ENABLED=true`, `SPRINGDOC_SWAGGER_UI_ENABLED=true`인 환경에서 `/v3/api-docs`, `/swagger-ui/index.html`로 제공한다.

## 인증 갱신

`POST /api/auth/refresh`에 `{ "refreshToken": "..." }`를 전송한다. 성공 응답의 `data`에는 `accessToken`, `refreshToken`이 들어간다.

갱신할 때 현재 refresh token은 즉시 교체되며 이전 토큰은 곧바로 유효하지 않게 된다. 프론트엔드는 여러 요청에서 동시에 401이 발생해도 refresh 요청을 single-flight로 한 번만 수행해야 한다.

| 상황 | HTTP | errorCode |
| --- | --- | --- |
| JWT 만료 | 401 | `AUTH_411` |
| 서명·위조·형식 오류 또는 유효하지 않은 refresh token | 401 | `AUTH_412` |

## CORS

`CORS_ALLOWED_ORIGINS`에 쉼표로 구분한 정확한 origin을 설정한다. 기본 개발 origin은 `http://localhost:3000,http://localhost:3001`이다. Bearer 인증은 쿠키를 쓰지 않으므로 `CORS_ALLOW_CREDENTIALS=false`가 기본이다. 허용 메서드는 GET/POST/PUT/PATCH/DELETE/OPTIONS, 허용 헤더는 Authorization/Content-Type/X-Request-Id이다.

## 권한

JWT `permissions`를 그대로 인가에 사용한다. `SUPER_ADMIN`은 다른 관리자 권한 검사를 통과하며, 다른 역할은 `ADMIN_ACCESS`와 해당 기능 권한이 모두 필요하다.

| 기능 | 읽기 | 변경 |
| --- | --- | --- |
| 서비스 의견 | `SUPPORT_MANAGE` | `SUPPORT_MANAGE` |
| 콘텐츠 검증 | `FEED_READ` | `FEED_MODERATE` |
| 매장 운영 | `STORE_READ` | `STORE_UPDATE` |
| 피드 검수 | `FEED_READ` | `FEED_MODERATE` |
| 시즌 큐레이션 | `SEASONAL_READ` | `SEASONAL_MANAGE` |

권한 부족은 HTTP 403 `AUTH_403`, 인증 누락은 HTTP 401 `AUTH_401`이다.

## 기능 API

- `/api/feedback`, `/api/admin/feedback`, `/api/admin/feedback/summary`
- `/api/admin/content-verifications/**`
- `/api/admin/stores/**`
- `/api/admin/feeds/**`
- `/api/admin/seasonal-curations/**`

상태 값:

- 의견: `received`, `in_progress`, `resolved`, `improvement_candidate`
- 검증: `pending`, `in_review`, `approved`, `rejected`, `changes_requested`
- 매장 운영: `operating`, `temporarily_closed`, `closed`
- 매장·피드 노출: `visible`, `hidden`
- 큐레이션: `DRAFT`, `SCHEDULED`, `PUBLISHED`, `ARCHIVED`

상태 변경 요청은 `reason`과 `version`을 받으며 검증 승인만 `reason`이 선택값이다. 관리자 변경은 `admin_audit_logs`에 전후 값, 사유, request ID와 함께 저장한다.

비로그인 의견의 선택 연락처는 `FEEDBACK_CONTACT_RETENTION_DAYS`(기본 90일) 뒤 UTC 일일 작업으로 자동 파기하며, 응답의 `contactPurgeAt`으로 예정 시점을 확인할 수 있다.

## 통합 검증 계정과 데이터

실제 비밀번호를 저장소에 커밋하지 않는다. 테스트 환경에서 가입 API로 계정을 만든 뒤 역할을 부여한다.

- 최고 관리자: `SUPER_ADMIN`
- 승인 담당자: `OPERATOR`
- 콘텐츠 담당자: `CONTENT_MANAGER`
- 조회 전용: `VIEWER`
- 사업자: `STORE_OWNER`
- 일반 회원: `USER`

세부 권한을 직접 시험하려면 `admin_user_permissions`에 활성 행을 추가하거나 `revoked_at`을 설정한다. 기능 데이터는 [admin-integration-test-data.sql](../db/admin-integration-test-data.sql)로 만들고 같은 파일의 cleanup 구문으로 정리한다. CORS는 [admin-api-smoke.ps1](../../scripts/admin-api-smoke.ps1)로 검증한다.
