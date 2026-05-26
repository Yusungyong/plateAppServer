# Backend Deep Structure Review

작성일: 2026-05-26  
대상: Plate Service Spring Boot backend  
범위: `src/main/java` 내부의 Controller, Service, Repository, Security, Common 계층 상호작용 중심 점검

## 1. 점검 방식

이번 문서는 단순 파일 목록 기반이 아니라, 실제 요청이 들어왔을 때 다음 흐름으로 이어지는지를 기준으로 다시 확인했다.

- HTTP 진입점: Controller, SecurityConfig, JWT Filter
- 업무 처리: Service 간 호출, 트랜잭션, 외부 저장소 연동
- 데이터 접근: JPA Repository, JdbcTemplate SQL, soft delete 조건
- 공통 처리: 응답 포맷, 예외 처리, CORS, S3, 알림 부수효과
- 대표 도메인: auth, profile, like, map, feed, video, restaurant, notification

정적 분석이므로 운영 DB 데이터 품질, 실제 트래픽, 권한 정책 히스토리는 별도 확인이 필요하다. 다만 Java 파일 간 호출 관계와 SQL 조건 차이는 이번 검토에 반영했다.

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

## 3. 핵심 흐름 요약

### 3.1 인증/권한 흐름

주요 파일:

- `auth/security/SecurityConfig.java`
- `auth/security/JwtAuthFilter.java`
- `auth/security/JwtProvider.java`
- `auth/domain/User.java`
- `auth/repository/UserRepository.java`

흐름:

1. 요청 진입 시 Spring Security 필터 체인이 먼저 동작한다.
2. `OPTIONS /**`는 인증 없이 통과하도록 설정되어 있어 CORS preflight는 통과 가능하다.
3. `JwtAuthFilter`가 Bearer 토큰을 파싱하고, `JwtProvider`가 access token 여부와 서명을 검증한다.
4. 사용자 조회는 `UserRepository`를 통해 수행된다.
5. JWT claim의 `roles`, `permissions`가 Spring Security authority로 변환된다.
6. 관리자 API는 `ADMIN_ACCESS`, `RESTAURANT_MANAGE` 등 authority 기반으로 차단된다.

확인 결과:

- 993 같은 숫자 권한을 프론트에서 직접 해석하지 않고, JWT claim의 의미 있는 role/permission을 사용하는 방향은 코드에 맞다.
- `JwtProvider`는 access token과 refresh token의 `typ` claim을 구분한다.
- `SecurityConfig`의 공개 경로와 `JwtAuthFilter`의 `PUBLIC_PATHS`가 완전히 같지는 않다. 예를 들어 SecurityConfig는 `/api/auth/**`를 공개하지만 Filter의 public matcher는 `/auth/**` 형태다. 토큰이 없으면 큰 문제는 없지만, 공개 API에 잘못된 Bearer 토큰이 붙으면 Filter에서 먼저 실패할 수 있는 여지가 있다.

권장:

- 공개 경로 정의를 한 곳으로 모으거나, 최소한 `/api/auth/**`, `/api/email/**`, Swagger 경로를 SecurityConfig와 JwtAuthFilter에서 동일하게 맞춘다.
- 권한 문자열은 enum 또는 상수 클래스로 묶어 Controller/Security/JWT 발급부가 같은 값을 쓰게 한다.

### 3.2 응답/예외 흐름

주요 파일:

- `common/api/ApiResponse.java`
- `common/dto/ApiResponse.java`
- `common/error/GlobalExceptionHandler.java`
- `auth/security/RestAuthenticationEntryPoint.java`
- `auth/security/RestAccessDeniedHandler.java`

확인 결과:

- 응답 래퍼가 2개 존재한다.
  - `common.api.ApiResponse`: `success`, `data`, `message`, `errorCode`, `requestId`, `timestamp`
  - `common.dto.ApiResponse`: `success`, `data`, `error { code, message }`
- `GlobalExceptionHandler`는 `common.api.ApiResponse`를 사용한다.
- 기존 일부 Controller는 `common.dto.ApiResponse`를 직접 반환한다.
- `StoreCommentController`처럼 Controller 내부에 개별 `@ExceptionHandler`를 둔 곳도 있다.

영향:

- 프론트에서 API별 성공/실패 응답 구조가 달라질 수 있다.
- 공통 에러 처리, requestId 추적, 문서화가 어려워진다.
- 신규 관리자 식당 API는 `common.api.ApiResponse` 계열이라 비교적 최신 패턴이다.

권장:

- 신규/수정 API부터 `common.api.ApiResponse`로 통일한다.
- `common.dto.ApiResponse`는 deprecated 대상으로 표시하고 단계적으로 제거한다.
- Controller별 예외 처리보다 `GlobalExceptionHandler` 중심으로 모은다.

### 3.3 좋아요, 마이페이지, 좋아요 지도 흐름

주요 파일:

- `profile/service/ProfileService.java`
- `profile/service/ProfileDetailService.java`
- `profile/service/ProfileActivityDetailService.java`
- `like/service/StoreLikeService.java`
- `like/service/FeedLikeService.java`
- `like/service/LikeService.java`
- `map/repository/MapNearbyRepository.java`

#### 마이페이지 좋아요 수

`ProfileDetailService.loadStats()`의 `likeCount`는 다음 조건으로 계산된다.

- `fp_50`: `username = :username`, `use_yn = 'Y'`, `deleted_at IS NULL`
- `fp_60`: `username = :username`, `use_yn = 'Y'`, `deleted_at IS NULL`
- 두 count를 단순 합산

즉, 사용자가 누른 좋아요 row 수를 세며, 좋아요 대상 콘텐츠가 현재 공개 상태인지, 삭제되었는지까지는 join으로 검증하지 않는다.

반면 `ProfileService.getUserStats()`는 `likesCount = 0`으로 고정되어 있다. 같은 마이페이지 계열이라도 어떤 API를 호출하느냐에 따라 좋아요 수가 다르게 나올 수 있다.

#### 좋아요 지도

`ProfileActivityDetailService.getLikedPlacesMap()`은 좋아요 row 수를 그대로 지도에 찍지 않는다.

동영상 좋아요 조건:

- `fp_50 l JOIN fp_300 s`
- 좋아요 row: `l.use_yn = 'Y'`, `l.deleted_at IS NULL`
- 영상/가게 row: `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL`
- 위치: `fp_310 loc`는 left join이며 `loc.use_yn = 'Y'`, `loc.deleted_at IS NULL`

이미지 좋아요 조건:

- `fp_60 l JOIN fp_400 f`
- 좋아요 row: `l.use_yn = 'Y'`, `l.deleted_at IS NULL`
- 이미지 feed row: `f.use_yn = 'Y'`
- 위치: `fp_310 loc`는 left join

그리고 결과는 `place_id` 또는 `store_name + address` fallback 기준으로 그룹핑된다.

결론:

- 마이페이지 숫자는 좋아요 row 수에 가깝다.
- 좋아요 지도 숫자는 현재 표시 가능한 장소 단위 그룹 수 또는 그룹별 합산 수에 가깝다.
- 좋아요 대상이 비공개, 삭제, 위치 없음, 같은 장소 중복이면 마이페이지 숫자와 지도 표현 수가 달라지는 것이 현재 코드상 자연스럽다.

추가로 확인된 차이:

- `FeedLikeService.toggleLike()`는 이미지 피드 좋아요 취소 시 row를 물리 삭제한다.
- `StoreLikeService.toggleLike()`는 `use_yn = 'N'`, `deleted_at` 세팅으로 soft delete 처리한다.
- 같은 좋아요 도메인인데 취소 방식이 다르다.

권장:

- “좋아요 총 개수”와 “지도에 표현 가능한 장소 개수”를 API 필드명으로 명확히 분리한다.
- `ProfileService.getUserStats()`의 `likesCount = 0`은 실제 count 로직으로 교체하거나 API 제거/비노출을 검토한다.
- `ProfileDetailService.getLikeCount()`도 지도와 같은 표시 정책을 따를지, 단순 누른 수로 둘지 정책을 문서화한다.
- 좋아요 취소 정책을 soft delete 또는 hard delete 중 하나로 통일한다.

### 3.4 일반 지도 마커 흐름

주요 파일:

- `map/controller/MapNearbyController.java`
- `map/service/MapNearbyService.java`
- `map/repository/MapNearbyRepository.java`
- `block`, `report` 관련 Repository

흐름:

1. Controller가 주변 좌표, 반경, 카테고리, 그룹 조건을 받는다.
2. Service가 차단/신고 사용자 목록을 구한다.
3. Repository가 `fp_310 loc JOIN fp_300 s` 기반으로 주변 가게 마커를 조회한다.
4. feed count는 `fp_400`의 `use_yn = 'Y'` 기준으로 붙인다.

확인 결과:

- 검색 API 쪽 SQL은 `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL` 조건이 있다.
- 반면 주변 마커 SQL의 candidates에는 동일한 `fp_300` 공개/삭제 필터가 보이지 않는다.
- 따라서 일반 지도 주변 마커와 검색 결과, 좋아요 지도 결과가 서로 다른 공개 정책으로 보일 수 있다.

권장:

- 주변 마커 SQL에도 `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL`을 명시한다.
- `fp_310 loc`에도 `loc.use_yn = 'Y'`, `loc.deleted_at IS NULL`을 candidates 조건에 명시하는 것이 안전하다.
- 지도 관련 SQL의 표시 정책을 `MapVisibilityPolicy` 같은 문서 또는 테스트로 고정한다.

### 3.5 Feed, Video, 파일 삭제 생명주기

주요 파일:

- `feed/service/ImageFeedUploadService.java`
- `video/service/VideoUploadService.java`
- `restaurant/service/RestaurantAdminService.java`
- `common/s3/S3UploadService.java`
- `common/s3/S3Config.java`

확인 결과:

- 영상/이미지 피드 도메인은 삭제 시 S3 파일, 썸네일, 댓글, 답글, 좋아요, 시청/방문/메뉴 참조를 함께 정리하는 로직이 있다.
- 프로필 이미지는 교체/삭제 시 S3 삭제를 수행한다.
- 관리자 식당 등록 API는 S3 업로드 후 `restaurant_media`, `restaurant_menu_media`에 URL/파일 메타데이터를 저장한다.
- 다만 관리자 식당 수정/삭제 시 기존 S3 파일을 삭제하는 흐름은 확인되지 않는다. DB row는 교체되지만 S3 객체는 남을 수 있다.

권장:

- 식당 미디어 교체/삭제 시 기존 `fileUrl`을 수집해 `S3UploadService.deleteObjectByUrl()` 호출을 추가한다.
- DB 트랜잭션과 S3 삭제는 완전한 원자성이 없으므로, 실패 시 재시도 가능한 orphan cleanup 배치도 고려한다.
- restaurant file path 정책은 `aws.s3.restaurantFilePath` 아래로 고정되어 있으므로 프론트는 응답의 `fileUrl`을 그대로 렌더링하면 된다.

### 3.6 알림 부수효과

주요 파일:

- `notification/service/NotificationCommandService.java`
- `like/service/StoreLikeService.java`
- `like/service/FeedLikeService.java`
- comment, friend 관련 service

확인 결과:

- 좋아요, 댓글, 친구 요청 등은 본 작업 이후 알림 생성 로직을 호출한다.
- 좋아요의 경우 자신이 자기 콘텐츠를 좋아요하면 알림을 보내지 않도록 필터링한다.
- 좋아요 중복 요청 또는 동시 요청이 발생할 때 알림 중복 생성 가능성은 추가 검증이 필요하다.

권장:

- 알림 생성은 도메인 이벤트처럼 idempotency key를 두거나, 같은 actor/recipient/type/target 조합의 중복 방지 정책을 검토한다.
- 좋아요 toggle의 DB 상태 변경과 알림 생성이 한 트랜잭션 안에서 일어나는 현재 구조는 단순하지만, 외부 push 발송이 붙으면 트랜잭션 이후 발송으로 분리하는 편이 안전하다.

## 4. 주요 리스크

### P0. 로컬 진단용 파일/로그에 민감 정보가 섞일 위험

루트에 진단용 Java 파일, class 파일, 서버 로그 파일, 로컬 Maven 디렉터리 등이 생겼던 이력이 있다. 특히 DB 접속 정보를 직접 넣은 임시 파일이 작업 중 만들어진 경우, git에 올라가면 즉시 보안 사고가 된다.

권장:

- 루트의 임시 실행 파일, class 파일, 로그 파일은 커밋 대상에서 제외한다.
- credential이 들어간 파일은 즉시 삭제하고, 이미 원격에 올라갔다면 DB 비밀번호를 교체한다.
- `.gitignore`에 `*.class`, `*.log`, `server-*.out.log`, `server-*.err.log`, 임시 진단 파일 패턴을 추가한다.

### P1. 운영 노출 설정이 개발 모드에 가깝다

`application.yaml` 기준:

- `server.error.include-stacktrace: always`
- `spring.jpa.show-sql: true`
- actuator endpoint exposure `include: "*"`
- Swagger/OpenAPI 경로 permitAll

영향:

- 운영 환경에서 stack trace, SQL, actuator 정보가 노출될 수 있다.

권장:

- `application-local.yaml`, `application-prod.yaml`로 분리한다.
- prod에서는 stacktrace 비노출, SQL 로그 off, actuator 최소 노출, Swagger 제한을 적용한다.

### P1. 좋아요 수/지도 수 불일치가 코드 정책상 발생한다

마이페이지, 상세 활동, 좋아요 지도, 일반 지도는 각각 다른 SQL과 필터를 사용한다.

영향:

- 사용자는 “좋아요 10개인데 지도에는 7개”처럼 인식할 수 있다.
- 실제로는 비공개/삭제/위치 없음/동일 장소 그룹핑 때문에 차이가 날 수 있으나, API가 이를 명확히 설명하지 않는다.

권장:

- `totalLikeCount`, `displayablePlaceCount`, `hiddenLikeCount` 같은 의미 있는 필드를 분리한다.
- 좋아요 지도 응답에 “좌표 없음” 항목 처리 정책을 명확히 한다.

### P1. 일반 주변 지도 마커 공개 조건 누락 가능성

`MapNearbyRepository`의 주변 마커 SQL은 검색 SQL과 달리 `fp_300` 공개/삭제 조건이 보이지 않는다.

영향:

- 삭제/비공개/비활성 영상 가게가 주변 마커에 남아 보일 수 있다.

권장:

- 주변 마커 candidates에 `s.use_yn = 'Y'`, `s.open_yn = 'Y'`, `s.deleted_at IS NULL`을 추가한다.
- 위치 테이블도 `loc.use_yn = 'Y'`, `loc.deleted_at IS NULL`을 적용한다.

### P1. 응답 포맷 이원화

두 종류의 `ApiResponse`가 동시에 쓰이고 있다.

영향:

- 프론트 공통 interceptor가 복잡해진다.
- 같은 오류라도 API별 파싱 방식이 달라질 수 있다.
- requestId 추적이 일부 API에서는 빠질 수 있다.

권장:

- `common.api.ApiResponse`를 표준으로 지정한다.
- `common.dto.ApiResponse` 사용 Controller를 우선순위별로 이전한다.

### P2. 식당 미디어 S3 orphan 가능성

관리자 식당 등록은 S3 저장과 DB 저장이 연결되어 있으나, 수정/삭제 시 기존 객체 삭제가 명확하지 않다.

영향:

- 프론트에서는 정상 삭제처럼 보여도 S3 비용과 객체가 누적될 수 있다.

권장:

- 식당 삭제 전 media URL 목록을 조회해 S3 삭제 처리한다.
- 수정 시 교체되는 media만 삭제하거나, cleanup job을 둔다.

### P2. 테스트 커버리지가 현재 규모 대비 낮다

현재 테스트 파일은 5개 수준이다.

우선 추가할 테스트:

- Security path matrix: 공개, 인증 필요, 관리자 권한 필요 API
- JWT role/permission claim 변환
- 좋아요 count와 좋아요 지도 count 정책
- 지도 주변 마커 공개/삭제 필터
- S3 업로드 path와 삭제 cleanup
- Restaurant CRUD create/update/delete cascade
- Feed/Video like toggle 동시성 또는 중복 요청

### P2. ID 생성 방식 일부 경쟁 조건 가능성

일부 Repository에서 `max(id) + 1` fallback 방식이 보인다.

영향:

- 동시 업로드 시 중복 ID가 발생할 수 있다.

권장:

- DB sequence 또는 identity 컬럼으로 이전한다.
- 기존 테이블 제약 때문에 즉시 변경이 어렵다면 insert retry 또는 advisory lock을 검토한다.

## 5. 우선순위별 개선 제안

### 1순위: 보안/운영 설정 분리

- prod profile에서 stacktrace, show-sql, actuator 전체 노출 제한
- Swagger 공개 여부 환경별 분리
- 임시 파일과 credential 포함 파일 정리

### 2순위: 좋아요/지도 정책 정리

- 마이페이지 좋아요 수의 기준 확정
- 좋아요 지도는 장소 그룹 기준임을 API 필드명으로 표현
- 일반 지도 마커 SQL에 공개/삭제 필터 추가

### 3순위: 응답 포맷 통일

- `common.api.ApiResponse`를 표준으로 정한다.
- 신규 API 문서에는 표준 응답만 안내한다.
- 기존 `common.dto.ApiResponse` 사용 Controller를 점진 이전한다.

### 4순위: 파일 생명주기 정리

- restaurant media 수정/삭제 시 S3 cleanup 추가
- orphan file 정리 배치 또는 관리자 점검 API 검토

### 5순위: 회귀 테스트 보강

- 좋아요/지도/권한/CORS/restaurant CRUD 위주로 먼저 테스트를 추가한다.
- 이후 profile/feed/video 삭제 cascade 테스트를 넓힌다.

## 6. 도메인별 관찰 메모

### auth

- JWT 기반 stateless 구조는 적절하다.
- refresh token 저장소가 있어 토큰 갱신/로그아웃/계정 삭제 시 제어 가능하다.
- public path 정의 중복은 정리 대상이다.

### profile

- 프로필 관련 API가 여러 Controller/Service로 나뉘어 있다.
- `ProfileService`, `ProfileDetailService`, `ProfileActivitySummaryService`, `ProfileActivityDetailService`, `MyProfileService`가 유사 데이터를 서로 다른 기준으로 만들 가능성이 있다.
- 좋아요 수, 방문 가게 수 같은 통계 필드는 단일 산출 클래스로 모으는 것이 좋다.

### like

- store like와 feed like의 취소 방식이 다르다.
- `fp_50`, `fp_60`에 대해 엔티티가 여러 이름으로 존재하는 구조라 정책 중복이 생기기 쉽다.
- 좋아요는 알림과 연결되어 있으므로 toggle idempotency가 중요하다.

### map

- 일반 지도, 검색 지도, 좋아요 지도가 서로 다른 SQL을 사용한다.
- 검색 쪽은 공개 필터가 비교적 명확하고, 주변 마커 쪽은 보강이 필요하다.
- 지도에 표시되는 수는 content row 수가 아니라 place/group 기준이라는 점을 API 이름과 문서에 반영해야 한다.

### feed/video

- S3, 이미지 처리, FFMPEG, 댓글, 좋아요, 방문 기록, 메뉴 참조까지 연결되어 있어 삭제 생명주기가 복잡하다.
- 현재 video/feed 쪽은 비교적 cleanup 의식이 들어가 있으나, 신규 restaurant media 쪽은 같은 수준까지 맞출 필요가 있다.

### restaurant

- 관리자 식당 CRUD는 프론트 요구사항을 처리할 수 있는 구조다.
- file upload 응답은 프론트가 그대로 `media.fileUrl`로 저장하면 된다.
- DB 저장은 `restaurants`, `restaurant_categories`, `restaurant_media`, `restaurant_menus`, `restaurant_menu_media` 계열로 나뉜다.
- 삭제/수정 시 S3 객체 정리 정책이 추가되면 운영 안정성이 좋아진다.

## 7. 결론

현재 백엔드는 기능별 확장은 많이 진행되어 있고, 서비스 분리도 어느 정도 되어 있다. 다만 규모가 커진 만큼 “같은 의미의 데이터”를 여러 Service와 SQL이 각자 계산하는 구간이 생겼다.

가장 먼저 잡아야 할 것은 세 가지다.

1. 운영 노출 설정과 임시 파일/credential 정리
2. 좋아요 수, 지도 표시 수, 공개/삭제 필터 기준 통일
3. 표준 응답 포맷과 예외 처리 통일

이 세 가지를 정리하면 프론트와의 계약이 훨씬 안정되고, 이후 restaurant/admin, profile, map, like 기능을 확장할 때 회귀 위험이 줄어든다.
