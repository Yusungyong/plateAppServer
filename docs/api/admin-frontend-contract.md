# Plate 관리자 프론트엔드 API 계약

기준일: 2026-06-22

이 문서는 현재 서버 소스 기준으로 프론트엔드에 공유할 관리자 API 계약을 정리한다. 기존 작업 요청서의 요구사항 중 구현이 확인된 항목과 소스와 달랐던 항목을 함께 반영했다.

## 공통 응답

API base URL은 배포 환경별 호스트 뒤에 `/api`를 붙인 값이다.

성공 응답은 `ApiResponse` 형식이다.

```json
{
  "success": true,
  "data": {},
  "requestId": "7f4c9f0d3b5a4a1b",
  "timestamp": "2026-06-22T00:00:00Z"
}
```

실패 응답도 같은 래퍼를 사용한다.

```json
{
  "success": false,
  "data": null,
  "message": "요청 값을 처리할 수 없습니다.",
  "errorCode": "COMMON_400",
  "requestId": "7f4c9f0d3b5a4a1b",
  "timestamp": "2026-06-22T00:00:00Z"
}
```

목록 응답은 기능별 DTO 안에 `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`를 포함한다. 관리자 변경 요청은 대체로 `version`을 받으며, 현재 값과 다르면 HTTP 409를 반환한다.

OpenAPI는 `SPRINGDOC_API_DOCS_ENABLED=true`, `SPRINGDOC_SWAGGER_UI_ENABLED=true` 환경에서 `/v3/api-docs`, `/swagger-ui/index.html`로 제공된다.

## 인증 갱신

`POST /api/auth/refresh`

```json
{
  "refreshToken": "..."
}
```

성공 응답의 `data`는 `accessToken`, `refreshToken`을 포함한다. refresh token은 서버에서 rotation되므로 프론트는 성공 응답의 새 refresh token을 저장해야 한다.

주요 인증 오류 코드는 다음과 같다.

| 상황 | HTTP | errorCode |
| --- | --- | --- |
| 인증 누락 | 401 | `AUTH_401` |
| 권한 부족 | 403 | `AUTH_403` |
| access token 만료 | 401 | `AUTH_402` |
| refresh token 만료 | 401 | `AUTH_411` |
| refresh token 형식 오류, 위조, 폐기 등 | 401 | `AUTH_412` |

여러 API에서 동시에 401이 발생할 수 있으므로 프론트는 refresh 요청을 single-flight로 처리한다.

## CORS

허용 origin은 `CORS_ALLOWED_ORIGINS`에 쉼표로 구분해 설정한다. 기본 개발 origin은 `http://localhost:3000,http://localhost:3001`이다.

Bearer 인증을 사용하므로 `CORS_ALLOW_CREDENTIALS=false`가 기본이다. 허용 메서드는 `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`이다. 허용 헤더는 `Authorization`, `Content-Type`, `X-Requested-With`, `X-Request-Id`이며, 서버는 응답 헤더로 `X-Request-Id`를 노출한다.

## 관리자 권한

서버는 JWT authority의 role과 permission을 사용한다. `SUPER_ADMIN`은 관리자 권한 검사를 통과한다. 그 외 관리자는 `ADMIN_ACCESS`와 기능별 permission이 모두 필요하다.

주요 role별 기본 permission은 다음과 같다.

| role | 기본 permission |
| --- | --- |
| `ADMIN`, `SUPER_ADMIN` | 전체 관리자 permission |
| `OPERATOR` | `ADMIN_ACCESS`, `DASHBOARD_READ`, `STORE_READ`, `STORE_APPROVE`, `STORE_UPDATE`, `SUPPORT_MANAGE` |
| `CONTENT_MANAGER` | `ADMIN_ACCESS`, `DASHBOARD_READ`, `FEED_READ`, `FEED_MODERATE`, `FEED_FEATURE`, `SEASONAL_READ`, `SEASONAL_MANAGE` |
| `VIEWER` | `ADMIN_ACCESS`, `DASHBOARD_READ`, `STORE_READ`, `FEED_READ`, `SEASONAL_READ`, `REPORT_READ` |

화면별 주요 permission은 다음과 같다.

| 기능 | 읽기 | 변경 |
| --- | --- | --- |
| 대시보드 | `DASHBOARD_READ` | 없음 |
| 서비스 의견 | `SUPPORT_MANAGE` | `SUPPORT_MANAGE` |
| 콘텐츠 검증 | `FEED_READ` | `FEED_MODERATE` |
| 입점 승인 | `STORE_READ` | `STORE_APPROVE` |
| 관리자 매장 운영 | `STORE_READ` | `STORE_UPDATE` |
| 피드 검수 | `FEED_READ` | `FEED_MODERATE` |
| 제철 큐레이션 | `SEASONAL_READ` | `SEASONAL_MANAGE` |
| 회원 모니터링 | `MEMBER_MONITORING_READ` | 없음 |

## 기능 API

현재 서버에 존재하는 관리자 API는 다음과 같다.

| 기능 | 경로 |
| --- | --- |
| 대시보드 | `GET /api/admin/dashboard/summary`, `GET /api/admin/dashboard/activity-trends`, `GET /api/admin/dashboard/region-distribution`, `GET /api/admin/activities` |
| 서비스 의견 | `POST /api/feedback`, `GET /api/admin/feedback`, `GET /api/admin/feedback/summary`, `PATCH /api/admin/feedback/{id}` |
| 콘텐츠 검증 | `GET /api/admin/content-verifications`, `GET /api/admin/content-verifications/{id}`, `GET /api/admin/content-verifications/{id}/history`, `PATCH /api/admin/content-verifications/{id}/assignee`, `POST /api/admin/content-verifications/{id}/approve`, `POST /api/admin/content-verifications/{id}/reject`, `POST /api/admin/content-verifications/{id}/request-changes` |
| 입점 승인 | `GET /api/admin/store-approvals`, `GET /api/admin/store-approvals/{applicationId}`, `GET /api/admin/store-approvals/{applicationId}/history`, `POST /api/admin/store-approvals/{applicationId}/approve`, `POST /api/admin/store-approvals/{applicationId}/hold`, `POST /api/admin/store-approvals/{applicationId}/request-changes`, `POST /api/admin/store-approvals/{applicationId}/reject`, `POST /api/admin/store-approvals/{applicationId}/documents/{documentId}/access-url` |
| 관리자 매장 운영 | `GET /api/admin/stores`, `GET /api/admin/stores/{id}`, `GET /api/admin/stores/{id}/history`, `PATCH /api/admin/stores/{id}/operation-status`, `PATCH /api/admin/stores/{id}/visibility` |
| 피드 검수 | `GET /api/admin/feeds`, `GET /api/admin/feeds/{id}`, `GET /api/admin/feeds/{id}/reports`, `POST /api/admin/feeds/{id}/hide`, `POST /api/admin/feeds/{id}/restore`, `PATCH /api/admin/feeds/{id}/recommendation` |
| 제철 큐레이션 | `GET /api/admin/seasonal-curations`, `POST /api/admin/seasonal-curations`, `GET /api/admin/seasonal-curations/{id}`, `PUT /api/admin/seasonal-curations/{id}`, `DELETE /api/admin/seasonal-curations/{id}`, `PATCH /api/admin/seasonal-curations/order`, `POST /api/admin/seasonal-curations/{id}/publish` |
| 회원 모니터링 | `GET /api/admin/member-monitoring/summary`, `GET /api/admin/member-monitoring/login-risks`, `GET /api/admin/member-monitoring/profile-changes`, `GET /api/admin/member-monitoring/risk-users` |

## 입점 승인 API

### 상태 값

`store_applications.approval_status`

- `draft`
- `pending`
- `on_hold`
- `approved`
- `rejected`

`store_applications.verification_status`

- `not_requested`
- `reviewing`
- `verified`
- `rejected`

사업자등록번호 원문은 저장하지 않는다. DB의 `business_number_encrypted`는 `bytea` 암호문이며, API 응답에는 마스킹된 `businessNumber`만 내려간다.

### 목록

`GET /api/admin/store-approvals`

query:

- `page`, `size`
- `keyword`: 매장명, 사업자번호 해시, 신청자명 검색
- `region`, `category`
- `status`
- `verificationStatus`
- `appliedFrom`, `appliedTo`
- `sort`: `appliedAt,desc`, `updatedAt,desc`, `id,desc` 등

응답 항목은 `id`, `name`, `categories`, `region`, `address`, `ownerName`, `approvalStatus`, `verificationStatus`, `appliedAt`, `updatedAt`을 포함한다.

### 상세

`GET /api/admin/store-approvals/{applicationId}`

응답은 신청 기본 정보, 마스킹된 사업자번호, 대표 메뉴, 문서 목록, `approvalStatus`, `verificationStatus`, `reviewReason`, `storeId`, `version`을 포함한다.

사업자용 상세 API `GET /api/owner/store-applications/{applicationId}`는 최신 반려 이력의 `reviewReasonCode`, `reviewReason`을 포함한다.

### 처리 이력

관리자용:

`GET /api/admin/store-approvals/{applicationId}/history`

신청자용:

- `GET /api/owner/store-applications/{applicationId}/history`
- `GET /api/business/applications/{applicationId}/history`

관리자용 응답은 `reviewId`, `previousStatus`, `nextStatus`, `reasonCode`, `reason`, `comment`, `reviewedBy`, `reviewedAt`, `requestId`, `changeRequestId`, `changeRequestStatus`, `applicantMessage`, `items`를 포함한다.

신청자용 응답은 내부 메모와 운영자 식별자를 제외하고 `reviewId`, `previousStatus`, `nextStatus`, `reasonCode`, `reason`, `reviewedAt`, `changeRequestId`, `changeRequestStatus`, `applicantMessage`, `items`를 포함한다.

보완 항목 `items[]`는 `id`, `field`, `label`, `reasonCode`, `message`, `editPath`, `displayOrder`를 포함한다.

### 승인

`POST /api/admin/store-approvals/{applicationId}/approve`

```json
{
  "version": 4,
  "comment": "검토 완료"
}
```

허용 현재 상태는 `pending`, `on_hold`, `rejected`이다. 승인하려면 `verificationStatus=verified`이고 모든 제출 문서가 `verified`여야 한다. 성공 시 `approvalStatus=approved`가 된다.

`rejected -> approved` 전환에서 `storeId`가 이미 있으면 기존 운영 매장을 중복 생성하지 않고 재활성화한다. 연결된 `store_owners` 이력이 있으면 `revoked_at`을 해제하고, 없으면 새 소유자 매핑을 만든다.

### 보류

`POST /api/admin/store-approvals/{applicationId}/hold`

```json
{
  "version": 4,
  "reason": "보완이 필요한 사유를 10자 이상 입력"
}
```

허용 현재 상태는 `pending`이다. 성공 시 `approvalStatus=on_hold`가 된다.

### 보완 요청

`POST /api/admin/store-approvals/{applicationId}/request-changes`

```json
{
  "version": 6,
  "items": [
    {
      "field": "store.address",
      "label": "매장 주소",
      "reasonCode": "ADDRESS_UNCLEAR",
      "message": "상세 주소와 층수를 확인할 수 없습니다.",
      "editPath": "/business/signup?applicationId=123&step=store"
    }
  ],
  "applicantMessage": "보완 항목을 수정한 뒤 다시 제출해 주세요."
}
```

허용 현재 상태는 `pending`, `on_hold`이다. 성공 시 `approvalStatus=on_hold`가 되며, 상태 변경 이력과 보완 항목이 함께 저장된다. 권한은 `STORE_APPROVE`이다.

### 재제출

신청자용:

- `POST /api/owner/store-applications/{applicationId}/resubmit`
- `POST /api/business/applications/{applicationId}/resubmit`

```json
{
  "version": 7
}
```

허용 현재 상태는 `on_hold`이다. 기존 신청 ID를 유지하고 `approvalStatus=pending`, `verificationStatus=reviewing`으로 전환한다. 열린 보완 요청은 `resubmitted`로 닫힌다.

### 반려

`POST /api/admin/store-approvals/{applicationId}/reject`

```json
{
  "version": 5,
  "reasonCode": "BUSINESS_INFO_MISMATCH",
  "reason": "신청 정보와 실제 운영 정보가 일치하지 않습니다."
}
```

허용 현재 상태는 `pending`, `on_hold`, `approved`이다. `reasonCode`는 `MISSING_DOCUMENT`, `INVALID_DOCUMENT`, `BUSINESS_INFO_MISMATCH`, `DUPLICATE_STORE`, `UNSUPPORTED_BUSINESS`, `OTHER` 중 하나다. 성공 시 `approvalStatus=rejected`가 된다.

`approved -> rejected` 전환에서는 연결된 운영 매장을 hard delete하지 않는다. 매장 노출 상태를 `hidden`으로 바꾸고 관리자 운영 상태를 `closed`로 전환하며, 활성 `store_owners`의 `revoked_at`을 설정한다. 이 처리는 같은 트랜잭션에서 수행된다.

### 문서 접근 URL

`POST /api/admin/store-approvals/{applicationId}/documents/{documentId}/access-url`

```json
{
  "purpose": "preview"
}
```

`purpose`는 `preview` 또는 `download`만 허용한다. 권한은 `STORE_READ`이다.

### 입점 승인 오류

| 상황 | HTTP | errorCode |
| --- | --- | --- |
| 신청 없음 | 404 | `STORE_APPROVAL_NOT_FOUND` |
| version 불일치 | 409 | `STORE_APPROVAL_VERSION_CONFLICT` |
| 허용되지 않는 상태 전환 | 409 | `STORE_APPROVAL_INVALID_TRANSITION` |
| 필수 문서 검증 미완료 | 409 | `STORE_APPROVAL_DOCUMENT_INCOMPLETE` |
| 사업자 검증 미완료 | 409 | `STORE_APPROVAL_VERIFICATION_INCOMPLETE` |
| 이미 승인된 동일 사업자번호 매장 존재 | 409 | `STORE_APPROVAL_DUPLICATE_STORE` |
| 문서 없음 | 404 | `STORE_DOCUMENT_NOT_FOUND` |

## 관리자 매장 운영 상태

입점 승인 후 운영 매장 상태는 `admin_store_operations`에서 관리한다.

- `operation_status`: `operating`, `temporarily_closed`, `closed`
- `visibility_status`: `visible`, `hidden`

소유자용 매장 공개 상태와 관리자 운영 상태는 분리되어 있다. 관리자 강제 변경은 `admin_audit_logs`에 기록된다.

## 제철 큐레이션

`/api/admin/seasonal-curations`는 제철 콘텐츠의 CRUD, 정렬 변경, 게시를 제공한다.

상태 값:

- `DRAFT`
- `SCHEDULED`
- `PUBLISHED`
- `ARCHIVED`

읽기는 `SEASONAL_READ`, 변경은 `SEASONAL_MANAGE` 권한이 필요하다.

## 검증 데이터와 점검

관리자 통합 테스트 데이터는 [admin-integration-test-data.sql](../db/admin-integration-test-data.sql)를 사용한다. 배포 후 DB 상태 점검은 [admin-p0-post-deploy-check.sql](../db/admin-p0-post-deploy-check.sql)을 사용한다.
