# Backend Deep Structure Review

작성일: 2026-05-26  
최종 업데이트: 2026-05-26  
대상: Plate Service Spring Boot backend  
범위: `src/main/java` 내부 Controller, Service, Repository, Security, Common 계층 상호작용 점검

## 1. 점검 방식

이번 점검은 단순 파일 목록이 아니라 실제 요청 흐름을 기준으로 확인했다.

- HTTP 진입점: Controller, SecurityConfig, JwtAuthFilter
- 업무 처리: Service 간 호출, 트랜잭션, 외부 저장소 연동
- 데이터 접근: JPA Repository, JdbcTemplate SQL, soft delete 조건
- 공통 처리: 응답 포맷, 예외 처리, CORS, S3, 알림 부수효과
- 대표 도메인: auth, profile, like, map, feed, video, restaurant, notification

운영 DB 데이터 품질, 실제 트래픽, 프론트 파싱 로직은 별도 확인 대상이다. 다만 Java 파일 간 호출 관계와 SQL 조건 차이는 이번 검토에 반영했다.

## 2. 프로젝트 규모

현재 백엔드는 중간 규모 이상의 서비스형 Spring Boot 프로젝트에 가깝다.

| 항목 | 규모 |
|---|---:|
| Java main 파일 | 약 381개 |
| Java test 파일 | 5개 |
| `src/main/java` 라인 수 | 약 29,981 lines |
| Java 전체 라인 수 | 약 30,460 lines |

주요 패키지 규모:

| 패키지 | 파일 수 | 라인 수 |
|---|---:|---:|
| `profile` | 34 | 3,193 |
| `video` | 22 | 2,889 |
| `auth` | 45 | 2,754 |
| `comment` | 29 | 2,340 |
| `feed` | 17 | 2,056 |
| `friend` | 26 | 1,882 |
| `common` | 23 | 1,852 |
| `recommendation` | 21 | 1,814 |
| `notification` | 20 | 1,576 |
| `home` | 14 | 1,566 |
| `like` | 23 | 1,198 |
| `restaurant` | 12 | 1,095 |

## 3. 현재 조치 현황

| 우선순위 | 항목 | 상태 | 메모 |
|---|---|---|---|
| 1 | 운영 노출 설정과 로컬 산출물 정리 | 완료 | `application.yaml` 기본값 안전화, Swagger/SQL/actuator/debug 로그 env 제어 |
| 1 | JWT public path 중복 정리 | 완료 | `SecurityPaths.PUBLIC_MATCHERS`를 `JwtAuthFilter`에서 재사용 |
| 2 | 좋아요/지도 숫자 정책 정리 | 일부 완료 | `ProfileService.getUserStats()`의 좋아요 0 고정값 제거, 표시 지표 분리는 협의 대기 |
| 3 | ApiResponse 이원화 정리 | 진행 중 | 저위험 Controller 전환 완료, `ProfileController`만 남음 |
| 4 | Restaurant media S3 cleanup | 완료 | 수정/삭제 시 기존 미디어 S3 객체 정리 |
| 5 | 회귀 테스트 보강 | 진행 중 | Restaurant media cleanup, 지도 필터, 프로필 좋아요 stats, 권한/JWT 테스트 실행 완료 |

관련 문서:

- `docs/api-response-migration-plan-2026-05-26.md`
- `docs/frontend-profile-api-response-check-2026-05-26.md`

## 4. 핵심 흐름 분석

### 4.1 인증/권한 흐름

주요 파일:

- `auth/security/SecurityConfig.java`
- `auth/security/JwtAuthFilter.java`
- `auth/security/JwtProvider.java`
- `auth/security/SecurityPaths.java`
- `auth/repository/UserRepository.java`

흐름:

1. 요청 진입 시 Spring Security 필터 체인이 먼저 동작한다.
2. `OPTIONS /**`는 인증 없이 통과한다.
3. `JwtAuthFilter`가 Bearer 토큰을 파싱한다.
4. `JwtProvider`가 access token 여부와 서명을 검증한다.
5. 사용자 존재 여부를 `UserRepository`로 확인한다.
6. JWT의 `roles`, `permissions`가 Spring Security authority로 변환된다.
7. 관리자 API는 `ADMIN_ACCESS`, `RESTAURANT_MANAGE` 등 authority 기반으로 차단된다.

조치 완료:

- `SecurityPaths.PUBLIC_MATCHERS`에 `/api/auth/**`, `/api/email/**`를 추가했다.
- `JwtAuthFilter`가 자체 public path 배열 대신 `SecurityPaths.PUBLIC_MATCHERS`를 재사용하도록 변경했다.
- 공개 경로 테스트도 `/api/auth/**`, `/api/email/**` 포함 기준으로 갱신했다.

남은 권장:

- `SecurityConfig`의 public endpoint 목록도 장기적으로 `SecurityPaths` 또는 별도 정책 클래스로 더 모을 수 있다.
- 권한 문자열은 현재 `PlateAuthorities` 기준으로 관리되고 있으므로, 신규 권한도 이쪽에 계속 모으는 편이 좋다.

### 4.2 운영 설정/노출 흐름

주요 파일:

- `src/main/resources/application.yaml`
- `src/main/resources/application-local.example.yaml`
- `.gitignore`

기존 리스크:

- `show-sql: true`
- `server.error.include-stacktrace: always`
- actuator endpoint `include: "*"`
- health detail `always`
- Swagger/OpenAPI 기본 활성
- 루트에 임시 진단 파일, class, log, 로컬 캐시, jar 파일이 추적될 수 있는 상태

조치 완료:

- `JPA_SHOW_SQL:false` 기본
- `SERVER_ERROR_INCLUDE_STACKTRACE:never` 기본
- actuator 기본 노출 `health,info`
- health detail 기본 `never`
- `SPRINGDOC_API_DOCS_ENABLED:false`
- `SPRINGDOC_SWAGGER_UI_ENABLED:false`
- debug/trace 로그는 env로만 켤 수 있게 변경
- `application-local.example.yaml` 추가
- `.gitignore`에 class/log/local Maven/Claude/진단 파일 패턴 추가
- 민감 정보가 들어갈 수 있는 임시 진단 파일과 로그성 산출물을 제거

주의:

- 로컬에서 Swagger가 필요하면 아래 env를 켜야 한다.

```text
SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

### 4.3 응답/예외 흐름

주요 파일:

- `common/api/ApiResponse.java`
- `common/dto/ApiResponse.java`
- `common/error/GlobalExceptionHandler.java`
- `auth/security/RestAuthenticationEntryPoint.java`
- `auth/security/RestAccessDeniedHandler.java`

현재 표준 후보:

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

기존 이전 대상:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "...",
    "message": "..."
  }
}
```

조치 완료:

아래 Controller는 `common.dto.ApiResponse`에서 `common.api.ApiResponse`로 전환했다.

- `StoreLikeController`
- `FriendManagementController`
- `ProfileDetailController`
- `ProfileActivitySummaryController`
- `ProfileActivityDetailController`

현재 남은 이전 응답 사용처:

- `ProfileController`

남긴 이유:

- `PUT /api/users/me/password`가 `ApiResponse.error(...)` 형태의 기존 오류 응답을 직접 반환한다.
- `DELETE /api/users/me`, `DELETE /api/users/me/social`은 `DeleteAccountResponse`를 직접 반환한다.
- 프론트가 `error.code`, `error.message` 또는 `DeleteAccountResponse` 구조를 직접 참조할 수 있어 협의 후 전환하는 편이 안전하다.

프론트 공유 완료:

- `docs/frontend-profile-api-response-check-2026-05-26.md` 문서로 프론트 확인 요청 내용을 작성했다.

### 4.4 좋아요, 마이페이지, 좋아요 지도 흐름

주요 파일:

- `profile/service/ProfileService.java`
- `profile/service/ProfileDetailService.java`
- `profile/service/ProfileActivityDetailService.java`
- `like/service/StoreLikeService.java`
- `like/service/FeedLikeService.java`
- `like/service/LikeService.java`
- `map/repository/MapNearbyRepository.java`

현재 차이:

- 마이페이지 상세의 `likeCount`는 `fp_50`, `fp_60` 활성 좋아요 row 수를 단순 합산한다.
- 좋아요 지도는 좋아요 대상 중 공개/활성 콘텐츠를 장소 기준으로 그룹핑한다.
- 같은 장소에 여러 개 좋아요가 있으면 지도 마커는 하나로 묶일 수 있다.
- 삭제/비공개/좌표 없음/장소 중복 때문에 마이페이지 숫자와 지도 표시 수가 달라질 수 있다.
- `ProfileService.getUserStats()`의 `likesCount = 0` 고정값은 제거했고, `fp_50`, `fp_60` 활성 좋아요 row 합산으로 변경했다.
- `FeedLikeService`는 이미지 좋아요 취소 시 row를 물리 삭제하고, `StoreLikeService`는 soft delete 처리한다.

프론트 공유 완료:

- 현재 차이는 코드 정책상 발생 가능한 정상 차이라는 설명을 프론트에 전달했다.
- 마이페이지 stats API의 `likesCount`가 항상 0으로 내려가던 문제는 수정했다.

남은 권장:

- `totalLikeCount`
- `displayableLikedPlaceCount`
- `hiddenLikedContentCount`

위처럼 지표를 분리할지 프론트와 협의 후 서버 응답을 보강한다.

### 4.5 일반 지도 마커 흐름

주요 파일:

- `map/controller/MapNearbyController.java`
- `map/service/MapNearbyService.java`
- `map/repository/MapNearbyRepository.java`

확인 결과:

- 검색 SQL은 `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL` 조건이 있다.
- 주변 마커 SQL의 candidates에도 동일한 `fp_300` 공개/삭제 필터를 추가했다.
- 위치 테이블도 `loc.use_yn = 'Y'`, `loc.deleted_at IS NULL` 조건을 추가했다.

권장:

- 지도 표시 정책을 문서화하고, 검색/주변/좋아요 지도 SQL 조건이 계속 맞는지 테스트를 넓힌다.

### 4.6 Feed, Video, Restaurant, S3 파일 생명주기

주요 파일:

- `feed/service/ImageFeedUploadService.java`
- `video/service/VideoUploadService.java`
- `restaurant/service/RestaurantAdminService.java`
- `restaurant/service/RestaurantAdminFileService.java`
- `common/s3/S3UploadService.java`

확인 결과:

- 영상/이미지 피드 도메인은 삭제 시 S3 파일, 썸네일, 댓글, 답글, 좋아요, 시청/방문/메뉴 참조를 함께 정리하는 로직이 있다.
- 프로필 이미지는 교체/삭제 시 S3 삭제를 수행한다.
- 관리자 식당 등록은 S3 업로드 후 `restaurant_media`, `restaurant_menu_media`에 URL/파일 메타데이터를 저장한다.
- 관리자 식당 수정/삭제 시 기존 S3 파일을 정리하도록 보강했다.
- 수정 시 기존 미디어 URL 중 새 요청에 다시 포함되지 않은 URL만 S3 삭제한다.
- 삭제 시 해당 식당의 기존 미디어 URL을 모두 S3 삭제 대상으로 처리한다.
- S3 삭제 실패는 DB 수정/삭제를 막지 않도록 경고 로그만 남긴다.

권장:

- S3 삭제 실패를 추후 재시도할 수 있는 orphan cleanup 배치 검토

### 4.7 알림 부수효과

주요 파일:

- `notification/service/NotificationCommandService.java`
- `like/service/StoreLikeService.java`
- `like/service/FeedLikeService.java`
- comment, friend 관련 service

확인 결과:

- 좋아요, 댓글, 친구 요청 등은 본 작업 이후 알림 생성 로직을 호출한다.
- 자신이 자기 콘텐츠를 좋아요하면 알림을 보내지 않도록 필터링한다.
- 좋아요 중복 요청 또는 동시 요청이 발생할 때 알림 중복 생성 가능성은 추가 검증이 필요하다.

권장:

- actor/recipient/type/target 조합의 중복 방지 정책 검토
- 외부 push 발송이 붙는 경우 트랜잭션 이후 발송으로 분리 검토

## 5. 주요 리스크와 상태

### P0. 로컬 진단 파일/로그/민감 정보 유입 위험

상태: 조치 완료

조치:

- 임시 진단 파일, class, log, jar, 로컬 캐시 추적 제거
- `.gitignore` 보강
- 작업트리에서 DB credential 문자열 검색 결과 없음 확인

남은 운영 권장:

- 이미 원격 저장소에 credential이 올라간 이력이 있다면 비밀번호 교체 필요
- 이력 정리 여부는 별도 판단 필요

### P1. 운영 노출 설정이 개발 모드에 가까움

상태: 조치 완료

조치:

- `show-sql`, stacktrace, actuator, Swagger, DEBUG 로그를 안전 기본값으로 변경
- 로컬 필요값은 env 또는 `application-local.yaml`에서만 켜도록 정리

### P1. 좋아요 수/지도 수 불일치

상태: 일부 완료, 프론트 협의 대기

현재 판단:

- 서버 기준상 마이페이지 상세 count와 좋아요 맵 count 차이는 발생 가능한 구조다.
- 프론트에는 “마이페이지는 좋아요 row 수, 좋아요 맵은 표시 가능한 장소 그룹 수”에 가깝다고 공유했다.
- `ProfileService.getUserStats()`의 `likesCount = 0` 고정값은 실제 활성 좋아요 count로 수정했다.

다음:

- 지표명 분리 여부 결정
- `ProfileService.getUserStats()`의 `likesCount = 0` 처리

### P1. 일반 주변 지도 마커 공개 조건 누락 가능성

상태: 조치 완료

조치:

- `MapNearbyRepository` 주변 마커 SQL 필터 보강
- `loc.use_yn = 'Y'`, `loc.deleted_at IS NULL` 추가
- `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL` 추가
- SQL 필터 유지 테스트 추가

### P1. 응답 포맷 이원화

상태: 진행 중

조치 완료:

- 저위험 Controller 5개 전환
- 전환 계획 문서 작성
- 프론트 확인 문서 작성

남음:

- `ProfileController`
- `DeleteAccountResponse` 직접 반환 정책 정리
- `common.dto.ApiResponse` 최종 제거

### P2. Restaurant media S3 orphan 가능성

상태: 조치 완료

조치:

- 수정/삭제 전 기존 media URL 조회
- 수정 시 제거된 URL만 `S3UploadService.deleteObjectByUrl()` 호출
- 삭제 시 전체 기존 media URL 삭제 호출
- S3 삭제 실패는 warn 로그로 남기고 DB 작업은 유지

남은 권장:

- 실패한 S3 삭제 재처리를 위한 cleanup job 또는 운영 점검 쿼리 추가

### P2. 테스트 커버리지 부족

상태: 진행 중

추가 완료:

- `RestaurantAdminServiceTest`
- 식당 수정 시 제거된 미디어 URL만 S3 삭제하는지 검증
- 식당 삭제 시 기존 미디어 URL을 모두 S3 삭제하는지 검증
- 수정 요청에서 재사용된 URL은 삭제하지 않는지 검증
- `MapNearbyRepositoryTest`
- 주변 지도 마커 SQL에 위치/스토어 공개, 삭제 필터가 포함되는지 검증
- `ProfileServiceTest`
- 마이페이지 stats 좋아요 수가 0 고정값이 아니라 활성 좋아요 count를 사용하는지 검증
- `SecurityPathsTest`, `JwtProviderTest`
- 권한 public path와 JWT role/permission 변환 검증

남은 우선 테스트:

- 좋아요 count와 좋아요 지도 count 정책
- SecurityConfig 전체 path matrix
- S3 upload/delete path

## 6. 다음 작업 추천

현재 기준 다음 작업은 아래 순서가 적절하다.

1. `ProfileController` 응답 표준화 여부에 대한 프론트 답변 대기
2. `ProfileController` 응답 전환
3. 좋아요 count와 좋아요 맵 count 정책 테스트 추가
4. 실패한 Restaurant S3 cleanup 재처리 방안 검토
5. SecurityConfig 전체 path matrix 테스트 보강

프론트 응답을 기다리지 않고 바로 진행 가능한 안전 작업은 좋아요 count/지도 count 정책 테스트와 SecurityConfig path matrix 테스트 보강이다.

## 7. 결론

초기 보고서에서 가장 우선순위가 높았던 운영 노출 설정과 로컬 산출물 정리는 완료됐다.  
응답 포맷 통일도 저위험 API부터 진행되어, 현재는 `ProfileController`만 남은 상태다.

이제 남은 핵심은 사용자 눈에 직접 보이는 데이터 정책이다.

- 좋아요 총 수와 지도 표시 수의 의미 분리
- 일반 지도 마커의 공개/삭제 필터 정합성
- 식당 미디어 삭제 시 S3 cleanup 재처리 정책

이 세 가지를 순서대로 정리하면 프론트 계약과 운영 데이터 품질이 한 단계 안정된다.
