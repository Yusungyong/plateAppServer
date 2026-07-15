# PlateApp 백엔드 서버 종합 검토 보고서

## 0. 보고서 메타데이터

| 항목 | 값 |
|---|---|
| 검토 기준일 | 2026-07-15 (Asia/Seoul) |
| 저장소 | plateAppServer |
| 기준 브랜치 / 커밋 | main / 6a830e09af74f9fd9c97e2c6282b8b505a1b138b |
| 기술 스택 | Java 17 target, Spring Boot 3.4.12, Spring MVC/Security/JPA, PostgreSQL, Flyway, AWS S3, Firebase |
| 검토 방식 | 전체 저장소 정적 분석, 빌드·테스트, JaCoCo 계측, 마이그레이션 대조, 배포 스크립트 검토, SBOM/공개 보안 권고 대조 |
| 산출물 성격 | 사람과 조치용 챗봇이 함께 사용하는 실행형 백로그 |
| 코드 변경 | 백엔드 호환성 유지 범위의 즉시 조치 반영; 본 보고서는 미해결 항목만 수록 |

### 중요 제한

- 운영 DB, 운영 로그, AWS IAM/S3 bucket policy, ALB/Nginx 설정, 실제 트래픽과 데이터는 열람하지 않았다.
- 따라서 운영 데이터 오염, 헤더 위조 가능성, S3 공개 범위처럼 배포환경에 의존하는 항목은 반드시 운영 확인 단계를 거쳐야 한다.
- 침투 테스트, 실제 계정 대상 공격, 대용량 부하 테스트는 수행하지 않았다.
- 의존성 스캔의 패키지-버전 일치는 취약점의 실제 도달 가능성을 곧바로 뜻하지 않는다. 아래에서 저장소 구성과 사용 경로를 기준으로 별도 분류했다.
- 이미 운영에 적용된 Flyway 파일은 수정하면 안 된다. 특히 V8/V9는 새 V13 이상 마이그레이션에서만 정정한다.

## 1. 결론

현재 코드는 빌드되고 추가된 회귀 테스트도 통과한다. 그러나 이를 운영 안전성의 근거로 보기는 어렵다. 전체 커버리지는 여전히 낮고, 핵심 권한·복구·동시성·PostgreSQL 마이그레이션 경로 다수가 테스트되지 않았다.

정적 증거만으로 확정한 P0는 없다. 다만 아래 P1은 배포 전 또는 즉시 핫픽스 계획에 넣어야 한다.

| 등급 | 그룹화된 발견사항 수 | 해석 |
|---|---:|---|
| P0 | 0 | 저장소 정적 증거만으로 현재 사고를 확정한 항목 없음 |
| P1 | 14 | 배포 게이트 또는 즉시 핫픽스 대상 |
| P2 | 28 | 1~3개 스프린트 내 계획 처리 대상 |
| P3 | 5 | 구조·관측·개발생산성 개선 |

이 수치는 같은 원인의 세부 증거를 하나의 실행 단위로 묶은 값이며, CVE 개수나 결함 인스턴스 개수와 같지 않다.

1. 공개 점주 신청이 클라이언트가 보낸 사업자 인증 성공 상태를 신뢰한다.
2. 공개 추천 API가 임의 username으로 피해자의 친구·방문 활동을 조회한다.
3. isPrivate가 실제 프로필·활동 조회 접근제어에 적용되지 않는다.
4. 이메일 변경과 재설정 흐름을 연결하면 탈취한 access token을 영구 계정탈취로 승격할 수 있다.
5. OTP가 재사용 가능하며, 세션 무효화가 완성되지 않았다.
6. 친구 요청 권한 모델이 깨져 있다.
7. V8의 store_id = restaurants.id 자동 매핑은 서로 독립적인 ID를 같은 것으로 간주해 잘못된 식당 연결을 만들 수 있다.
8. DB와 S3 변경 및 알림 전달이 장애 상황에서 안전하지 않다.
9. 현재 해석된 Spring Security 6.4.13 등에는 기준일 현재 공식 보안 권고에 해당하는 버전이 포함된다.

### 릴리스 판단

- 신규 기능 위주의 대규모 릴리스: 보류 권고
- P1 보안·데이터 무결성 핫픽스: 즉시 착수
- V8이 운영에 적용됐다면: 쓰기 마이그레이션 전에 읽기 전용 데이터 감사가 선행되어야 함
- 기존 기능 유지 배포가 불가피하다면: SEC-04, BUS-01, SEC-01/02, APP-01을 우선 차단하고 DEP-01 최소 패치를 적용한 뒤 진행

## 2. 검증 결과와 규모

| 항목 | 결과 |
|---|---:|
| main Java 소스 | 476개, 약 35,292줄 |
| test Java 소스 | 37개, 약 2,931줄 |
| 컨트롤러 | 61개 |
| 매핑된 HTTP endpoint | 애플리케이션 약 301개 + Actuator probe 2개 |
| Flyway migration | V1~V12, 12개 |
| 테스트 | 96 성공 / 0 실패 / 0 오류 / 0 스킵 |
| 패키징 | clean verify 및 CycloneDX SBOM 생성 성공 |
| 실행 JDK | 로컬 JDK 21.0.6, 컴파일 target은 release 17 |
| 라인 커버리지 | 2,804 / 14,259 = 19.66% |
| 브랜치 커버리지 | 660 / 8,247 = 8.00% |
| 메서드 커버리지 | 18.96% |
| 클래스 커버리지 | 45.34% |
| 직접 의존성 | 22개 |
| 생성 SBOM component | 232개 |

커버리지가 특히 낮은 서비스 영역은 friend 약 0.8%, comment 약 0.9%, recommendation 약 1.5%, search 약 2.6%, feed 약 3.5%, profile 약 6.0%, notification 약 6.1% 수준이다. 영상 서비스는 이번 회귀 테스트로 약 10.7%까지 올랐지만 나머지 분기 다수가 여전히 검증되지 않았다.

컴파일 시 [SocialAuthService.java](../src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java)는 unchecked/unsafe operation 경고를 발생시켰다. 테스트의 MapNearby 영역에도 unchecked 경고가 있다.

## 3. 우선순위 정의

| 등급 | 의미 | 처리 원칙 |
|---|---|---|
| P0 | 현재 악용·데이터 손상 또는 전면 장애가 확인됨 | 즉시 차단·롤백·사고 대응 |
| P1 | 악용 가능성이 높거나 권한·데이터 무결성·가용성에 큰 영향 | 신규 기능보다 우선, 배포 게이트 |
| P2 | 정확성·성능·운영 안정성에 유의미한 위험 | 계획된 1~3개 스프린트 안에 처리 |
| P3 | 구조·관측·개발생산성 개선 | P1/P2 회귀 테스트 확보 후 점진 처리 |

## 4. 첫 72시간 실행 순서

### 0~4시간: 노출 차단과 운영 확인

1. SEC-04 공개 추천 BOLA를 API gateway 또는 코드에서 임시 차단한다. 익명 요청의 username을 무시하고 FRIEND surface를 비활성화한다.
2. BUS-01 점주 신청 요청의 verification 계열 필드를 무시한다. 임시로 제출 시 NTS를 서버에서 재검증하고, NTS 확인이 불가능하면 사업자등록증 필수 + 명시적 수동 미검증 심사로 보내거나 제출 자체를 중단한다.
3. SEC-01 일반 프로필 수정에서 email과 phoneNumber를 임시 제거하거나 별도 재인증 endpoint만 허용한다.
4. V8 운영 적용 여부와 의심 매핑 건수를 10절의 읽기 전용 SQL로 확인한다.
5. 운영 프록시가 외부 X-Forwarded-For를 제거하고 신뢰 프록시 주소만 전달하는지 확인한다.
6. 운영 S3 public access block, lifecycle, IAM 최소권한을 확인한다.

### 4~24시간: 회귀 테스트와 최소 핫픽스

1. SEC-01~04, BUS-01, APP-01의 실패 재현 통합 테스트를 먼저 작성한다.
2. 비밀번호 재설정 시 OTP 원자 소비, 모든 refresh 삭제, tokenVersion 증가를 적용한다.
3. 이메일 변경을 pending-email + 새 주소 OTP + 재인증 흐름으로 분리한다.
4. 친구 요청의 발신자/수신자 조건을 강제한다.
5. 현재 지원되는 Spring Boot 라인으로 올리고 해석 버전을 DEP-01 기준과 대조한다.

### 24~72시간: 데이터·운영 안정화

1. V8 감사 결과에 따라 수동 검토 가능한 correction table과 V13 이상 정정 마이그레이션을 설계한다.
2. S3 보상 정리와 알림 outbox 작업을 각각 분리한다.
3. PostgreSQL Testcontainers 기반 empty-schema Flyway 검증을 CI에 추가한다.
4. P1 수정 PR마다 Java 17 clean verify, PostgreSQL 통합 테스트, 권한 부정 테스트를 필수화한다.

## 5. P1 상세 발견사항

### SEC-01 — 일반 프로필 이메일 변경으로 영구 계정탈취 가능

- 확신도: 높음
- 근거:
  - [ProfileController.java:49](../src/main/java/com/plateapp/plate_main/profile/controller/ProfileController.java#L49)는 인증 사용자에게 PUT /api/users/me를 허용한다.
  - [UpdateProfileRequest.java:23](../src/main/java/com/plateapp/plate_main/profile/dto/UpdateProfileRequest.java#L23)는 email을 일반 프로필 필드로 받는다.
  - [ProfileService.java:100](../src/main/java/com/plateapp/plate_main/profile/service/ProfileService.java#L100)과 115~117은 현재 비밀번호, 최근 재인증, 새 이메일 OTP 없이 user.setEmail을 실행한다.
- 공격 경로: 탈취한 단기 access token → 공격자 이메일로 변경 → 해당 이메일로 OTP 수신 → reset-password → 장기 계정 장악.
- 영향: 계정탈취 지속성 확보, 사용자 연락처 변조, 기존 세션 유지.
- 조치:
  - email/phone을 UpdateProfileRequest에서 제거한다.
  - pendingEmail을 저장하고 현재 비밀번호 또는 MFA 재인증과 새 주소 OTP를 모두 통과한 뒤 반영한다.
  - 이전 주소와 새 주소에 변경 알림을 보내고, 성공 시 refresh 전량 폐기와 tokenVersion 증가를 수행한다.
  - 이메일 unique 충돌을 409로 변환한다.
- 완료 기준:
  - access token만으로 이메일을 바꿀 수 없다.
  - OTP는 새 주소·사용자·목적·발급 ID에 결속된다.
  - 변경 직후 과거 access/refresh가 모두 401이다.
  - 동시 이메일 변경과 중복 이메일 테스트가 통과한다.

### SEC-02 — OTP·비밀번호 재설정·rate limit 결합 취약점

- 확신도: 높음. 단, XFF 우회 가능성은 운영 프록시 설정에 의존한다.
- 근거:
  - [RateLimitService.java:17](../src/main/java/com/plateapp/plate_main/common/security/RateLimitService.java#L17)은 프로세스 메모리 Map을 사용하고 43~60에서 첫 X-Forwarded-For 값을 신뢰한다.
  - [EmailVerification.java:25](../src/main/java/com/plateapp/plate_main/common/email/entity/EmailVerification.java#L25)는 코드를 평문으로 보관한다.
  - [EmailVerifyServiceImpl.java:113](../src/main/java/com/plateapp/plate_main/common/email/service/impl/EmailVerifyServiceImpl.java#L113)은 인증 후 isVerified만 설정하고 코드를 소비하거나 실패 횟수를 기록하지 않는다.
  - 같은 파일 65~108은 SMTP 실패를 삼킨 뒤에도 OTP를 활성 저장한다.
  - [PasswordResetServiceImpl.java:34](../src/main/java/com/plateapp/plate_main/auth/service/impl/PasswordResetServiceImpl.java#L34)는 검증된 동일 코드를 재사용하며 refresh 삭제와 tokenVersion 증가를 하지 않는다.
  - [PasswordResetController.java:51](../src/main/java/com/plateapp/plate_main/auth/controller/PasswordResetController.java#L51)의 새 비밀번호는 NotBlank뿐이다.
- 조치:
  - OTP 원문 대신 keyed hash를 저장한다.
  - purpose, subject, issuanceId, attempts, expiresAt, consumedAt을 가진 OTP 모델로 바꾼다.
  - 검증과 비밀번호 변경을 한 트랜잭션의 조건부 UPDATE로 원자 소비한다.
  - Redis 등 공유 저장소에서 계정·이메일·신뢰 IP·발급 ID별 제한을 둔다.
  - 프록시 신뢰 목록을 명시하고 애플리케이션이 임의 forwarded header를 직접 해석하지 않게 한다.
  - 비밀번호 정책을 8~64자 등 한 규칙으로 통일하고 재설정 후 모든 세션을 폐기한다.
- 완료 기준:
  - 동일 OTP 두 번째 사용 거부, 목적 교차 사용 거부, 5회 실패 후 잠금.
  - 다중 인스턴스와 임의 XFF에서도 rate limit이 공유된다.
  - SMTP 실패 시 활성 OTP가 남지 않는다.
  - 재설정 후 이전 access/refresh가 모두 거부된다.

### SEC-03 — 역할·비밀번호 변경 후 세션 무효화가 작동하지 않음

- 확신도: 높음
- 근거:
  - [User.java:65](../src/main/java/com/plateapp/plate_main/auth/domain/User.java#L65)에 tokenVersion이 있고 [JwtAuthFilter.java:78](../src/main/java/com/plateapp/plate_main/auth/security/JwtAuthFilter.java#L78)이 비교하지만, main 코드에서 setTokenVersion 호출은 확인되지 않았다.
  - 인증 비밀번호 변경은 refresh만 지우고, 재설정은 refresh도 지우지 않는다.
  - access token은 역할·권한을 claim에 담고 1시간, refresh는 14일이다.
  - [AuthService.java:150](../src/main/java/com/plateapp/plate_main/auth/service/AuthService.java#L150)의 refresh 회전은 발급 당시 tokenVersion을 검증하지 않는다.
- 영향: 비밀번호 탈취 대응, 관리자 강등, 점주 권한 회수 뒤에도 과거 access token이 계속 동작한다.
- 조치:
  - 비밀번호, 이메일, 역할, 관리자 권한, 점주 권한 변경마다 tokenVersion을 원자 증가시킨다.
  - refresh 세션에 발급 당시 tokenVersion 또는 session family ID를 저장한다.
  - 보안 변경 시 모든 refresh family를 폐기한다.
  - logout-all과 device별 logout endpoint를 제공한다.
- 완료 기준: 각 보안 이벤트 직후 과거 access/refresh가 401이고, 새 로그인만 성공한다.

### SEC-04 — 공개 추천 API BOLA와 친구 활동 노출

- 확신도: 높음
- 근거:
  - [SecurityConfig.java:67](../src/main/java/com/plateapp/plate_main/auth/security/SecurityConfig.java#L67)은 GET /api/recommendations를 공개한다.
  - [RecommendationController.java:21](../src/main/java/com/plateapp/plate_main/recommendation/controller/RecommendationController.java#L21)은 임의 username을 받는다.
  - [RecommendationQueryService.java:665](../src/main/java/com/plateapp/plate_main/recommendation/service/RecommendationQueryService.java#L665)는 익명 호출에서 usernameParam을 실제 사용자 주체로 채택한다.
  - 같은 파일 195~238은 해당 사용자의 최근 방문 매장과 친구 이름 최대 3개를 조립한다.
- 영향: 인증 없이 피해자 username만으로 방문·친구 활동과 매장을 조회할 수 있다.
- 즉시 완화: 익명 요청의 username을 무조건 무시하고 익명 FRIEND surface를 제거한다.
- 영구 조치:
  - 인증 사용자는 principal만 사용하고 요청 username을 권한 근거로 사용하지 않는다.
  - 게스트 개인화는 서버 발급 고엔트로피 guest ID와 별도 비민감 신호만 사용한다.
  - 친구 이름 노출은 명시적 개인정보 정책과 관계 확인 뒤에만 허용한다.
- 완료 기준: 익명 또는 A 사용자가 B username을 보내도 B의 방문·친구 데이터가 응답·쿼리에 사용되지 않는다.

### SEC-05 — isPrivate가 실제 접근제어에 사용되지 않음

- 확신도: 높음
- 근거:
  - [User.java:62](../src/main/java/com/plateapp/plate_main/auth/domain/User.java#L62)에 isPrivate가 있다.
  - [ProfileActivityDetailController.java:28](../src/main/java/com/plateapp/plate_main/profile/controller/ProfileActivityDetailController.java#L28)은 경로 username만으로 다른 사용자의 영상·이미지·좋아요 목록을 조회한다.
  - 프로필·통계·활동 조회 서비스에서 isPrivate 기반 공통 정책 적용을 찾지 못했다.
- 영향: 로그인 계정 하나로 비공개 사용자의 좋아요 식당, 주소, 활동 성향이 노출될 수 있다.
- 조치:
  - ProfileVisibilityPolicy를 단일 컴포넌트로 만들고 본인, 친구 상태, 차단 양방향, 비공개 상태를 모든 조회에 적용한다.
  - 좋아요 목록은 별도 동의가 없으면 본인 전용을 기본값으로 한다.
  - repository 쿼리 단계에서 필터링해 서비스 누락을 막는다.
- 완료 기준: 본인/친구/비친구/차단/관리자 조합별 정책 테스트가 모든 프로필 endpoint에서 통과한다.

### SEC-06 — 관리자 역할 상승과 권한 회수 지연

- 확신도: 높음
- 근거:
  - [SecurityConfig.java:108](../src/main/java/com/plateapp/plate_main/auth/security/SecurityConfig.java#L108)은 ADMIN_ACCOUNT_MANAGE 보유자에게 /api/users/detail 하위를 허용한다.
  - [UserDetailController.java:93](../src/main/java/com/plateapp/plate_main/profile/controller/UserDetailController.java#L93)은 동일 권한으로 임의 사용자의 role을 수정한다.
  - [UserUpdateService.java:40](../src/main/java/com/plateapp/plate_main/profile/service/UserUpdateService.java#L40)은 enum allowlist, 자기 상승 금지, SUPER_ADMIN 전용 규칙 없이 문자열을 그대로 저장한다.
  - 권한 변경 시 tokenVersion 증가와 보안 감사 이벤트가 없다.
- 영향: 계정관리 권한 보유자가 자신 또는 공모 계정을 SUPER_ADMIN으로 승격할 수 있고, 강등된 토큰은 만료까지 기존 claim을 유지한다.
- 조치:
  - 역할을 enum/DB check로 제한한다.
  - SUPER_ADMIN 부여·회수는 기존 SUPER_ADMIN만 가능하게 하고 자기 승인, 마지막 SUPER_ADMIN 제거를 막는다.
  - before/after, actor, reason, request ID를 불변 감사 로그에 남긴다.
  - 변경과 동시에 tokenVersion 증가와 refresh 폐기를 수행한다.
- 완료 기준: 일반 ADMIN/OPERATOR의 SUPER_ADMIN 부여는 403, 강등 즉시 기존 토큰은 401이다.

### BUS-01 — 클라이언트가 사업자 인증 성공을 위조 가능

- 확신도: 높음
- 근거:
  - [SecurityConfig.java:57](../src/main/java/com/plateapp/plate_main/auth/security/SecurityConfig.java#L57)과 [OwnerStoreApplicationController.java:32](../src/main/java/com/plateapp/plate_main/owner/controller/OwnerStoreApplicationController.java#L32)은 signup application 생성 경로를 공개한다.
  - [OwnerApplicationDtos.java:33](../src/main/java/com/plateapp/plate_main/owner/dto/OwnerApplicationDtos.java#L33)은 verificationProvider, verificationStatus, verificationVerifiedAt, verificationMessage를 클라이언트에서 받는다.
  - [OwnerStoreApplicationService.java:530](../src/main/java/com/plateapp/plate_main/owner/service/OwnerStoreApplicationService.java#L530)은 값을 그대로 정규화하고 318~356에서 저장한다.
  - 같은 파일 557~565는 상태 문자열이 verified이면 사업자등록증 없이 제출을 허용한다.
- 영향: NTS 확인 또는 증빙 없이 심사 대기 상태까지 진입해 사기 방지·심사 데이터의 신뢰 경계를 무너뜨린다. 최종 관리자 승인은 남지만 심사 부하와 오판 위험이 생긴다.
- 조치:
  - 서버 관리 verification 필드를 외부 요청 DTO에서 제거하고 unknown field 거부를 고려한다.
  - NTS 성공 결과를 사용자, 정규화 사업자번호 hash, nonce, 발급시각에 결속해 서버 레코드로 저장한다.
  - 짧은 만료와 일회성 consume을 적용하고 제출 시 서버 레코드만 조회한다.
  - 서버 proof 저장 기능을 만들기 전 임시 정책은 제출 시 NTS 재검증이다. NTS가 불가능하면 증빙문서 필수 + 수동 미검증 queue 또는 제출 중단 중 하나를 제품 정책으로 선택한다. 정상 신청을 조용히 전부 차단하는 구현은 피한다.
  - 증빙 대체 정책과 관리자 화면에 검증 출처를 명확히 표시한다.
- 완료 기준: forged verified payload는 무시 또는 400, NTS/유효 증빙 없는 제출은 항상 실패한다.

### APP-01 — 친구 요청의 발신자·수신자 및 상태 전이 모델 오류

- 확신도: 높음
- 근거:
  - [FriendManagementService.java:119](../src/main/java/com/plateapp/plate_main/friend/service/FriendManagementService.java#L119)은 요청 행 username에 발신자를 저장한다.
  - 같은 파일 105~115는 수신 요청을 recipient username으로 조회해 실제 수신자가 보지 못한다.
  - 173~217의 승인·거절 권한은 행 username, 즉 발신자에게 부여된다.
  - [FriendController.java:154](../src/main/java/com/plateapp/plate_main/friend/controller/FriendController.java#L154)의 레거시 API는 발신자가 임의 상태 문자열을 설정할 수 있다.
- 영향: 정상 승인 실패, 발신자의 자기 승인 가능성, 중복 관계와 알림 오류.
- 조치:
  - requester, recipient, enum status를 가진 canonical 모델 하나로 통합한다.
  - recipient만 accept/reject, requester만 cancel하도록 상태 전이를 명시한다.
  - 레거시 임의 상태 API를 제거한다.
  - 정규화 사용자 쌍과 활성 요청에 unique invariant를 둔다.
- 완료 기준: A→B 요청을 B만 조회·승인할 수 있고 A 승인 시도는 403, 동시 중복은 한 건만 생성된다.

### DB-01 — V8의 숫자 ID 자동 매핑으로 잘못된 식당 연결 가능

- 확신도: 코드상 높음, 운영 영향은 운영 DB 감사 필요
- 근거:
  - [V8__fp300_restaurant_mapping.sql:24](../src/main/resources/db/migration/V8__fp300_restaurant_mapping.sql#L24)은 fp_300.store_id = restaurants.id이면 이름·주소 확인 없이 restaurant_id를 설정한다.
  - 영상 store_id와 restaurants.id는 서로 별도로 생성되는 식별자 계열이며 두 값이 같다는 보장이 없다.
  - 이후 이름·주소 매칭은 restaurant_id가 null인 행만 다루므로 잘못 채운 숫자 매핑을 교정하지 않는다.
  - [OwnerStoreAnalyticsService.java:482](../src/main/java/com/plateapp/plate_main/owner/service/OwnerStoreAnalyticsService.java#L482)는 restaurant_id를 소유권·분석 연결에 사용한다.
- 영향: 다른 식당의 영상·분석이 연결되어 점주 분석 오염, 잘못된 콘텐츠 귀속, 잠재적 권한 경계 훼손이 발생할 수 있다.
- 금지사항: 운영에 V8이 적용됐다면 V8 파일을 수정하거나 단순히 모든 restaurant_id를 null로 되돌리지 않는다. 어떤 행이 V8에서 채워졌는지 provenance가 없기 때문이다.
- 조치:
  - 10절의 읽기 전용 SQL로 적용 여부와 의심 행을 export한다.
  - place_id, 사업자번호, 승인 신청, 관리자 확정표 같은 신뢰 가능한 키로 correction table을 만든다.
  - V13 이상에서 명시적으로 검증된 행만 수정하고 전후 수량, 샘플, orphan을 기록한다.
  - 향후 연결은 FK ID를 생성 시점부터 명시적으로 전달하고 이름·주소 fuzzy match를 권한 근거로 사용하지 않는다.
- 완료 기준: 모든 연결에 출처가 있고, 불일치 0건, 소유자별 표본 검증, 롤백 가능한 correction log가 존재한다.

### DB-02 — Flyway만으로 새 DB·재해복구 DB를 재현할 수 없음

- 확신도: 높음
- 근거:
  - 정적 대조상 엔티티가 참조하는 약 67개 테이블 중 46개가 Flyway 생성 대상으로 확인되지 않았다.
  - V1은 fp_100 등 기존 테이블이 있다고 가정하고, restaurants도 생성하지 않지만 V6/V10이 참조한다.
  - recipe와 home impression 일부 SQL은 deploy/docs에 별도 존재해 canonical migration에서 벗어나 있다.
  - 애플리케이션은 ddl-auto=validate와 Flyway enabled이므로 기존 수동 스키마에 의존한다.
- 영향: 새 환경, DR, preview, 테스트 DB를 동일하게 재현할 수 없고 운영 drift를 발견하기 어렵다.
- 조치:
  - 운영 catalog/검증된 schema dump를 기준으로 canonical baseline을 만든다.
  - 기존 설치는 baseline 전략과 checksum 정책을 문서화하고, 새 설치는 빈 PostgreSQL에서 전 migration이 실행되게 한다.
  - docs/deploy의 분산 SQL을 Flyway로 승격하고 소유자를 정한다.
  - CI에서 empty DB migrate → Hibernate validate → 핵심 smoke를 실행한다.
- 완료 기준: 빈 PostgreSQL 한 개에서 수동 SQL 없이 애플리케이션이 기동하고 schema diff가 0이다.

### INT-01 — DB 트랜잭션과 S3 변경이 비원자적

- 확신도: 높음
- 근거:
  - [VideoUploadService.java:76](../src/main/java/com/plateapp/plate_main/video/service/VideoUploadService.java#L76)은 S3 업로드 뒤 DB를 저장하고, 298~310은 업로드 뒤 길이 검증을 한다.
  - 교체는 DB commit 전에 기존 객체를 삭제하고, 삭제 API는 DB보다 S3를 먼저 지운다.
  - [OwnerStoreApplicationService.java:223](../src/main/java/com/plateapp/plate_main/owner/service/OwnerStoreApplicationService.java#L223), [ProfileService.java:135](../src/main/java/com/plateapp/plate_main/profile/service/ProfileService.java#L135)에도 같은 패턴이 있다.
- 영향: DB rollback 시 신규 고아 파일, 후반 DB 실패 시 기존 파일 영구 유실, DB가 없는 객체를 가리키는 상태가 생긴다.
- 조치:
  - 업로드 전에 크기·magic byte·duration·codec을 검증한다.
  - 신규 객체를 staging key로 올리고 DB에는 PENDING/READY 상태를 둔다.
  - DB commit 후 publish 또는 durable outbox/worker로 전환한다.
  - 기존 객체 삭제는 commit 후 재시도 가능한 GC tombstone으로 처리한다.
  - bucket lifecycle로 오래된 staging을 보조 정리한다.
- 완료 기준: DB save/commit 실패, S3 put/delete 실패, 프로세스 중단을 주입해 기존 파일 보존과 eventual cleanup을 확인한다.

### RES-01 — 업로드·FFmpeg·외부 OAuth 경로의 자원 고갈

- 확신도: 높음
- 근거:
  - [OwnerFileController.java:26](../src/main/java/com/plateapp/plate_main/owner/controller/OwnerFileController.java#L26)은 일반 인증 사용자도 owner file upload를 호출할 수 있다.
  - 전역 multipart 한도는 파일 100MB, 요청 150MB다.
  - [VideoUploadService.java:271](../src/main/java/com/plateapp/plate_main/video/service/VideoUploadService.java#L271)은 큰 임시 파일과 트랜스코딩을 요청 흐름에서 수행한다.
  - SocialAuthService는 외부 호출을 클래스 수준 DB transaction 안에서 수행한다.
- 영향: 무료 계정으로 CPU·임시 디스크·S3 비용을 소진하거나, 외부 공급자 지연으로 servlet thread와 DB connection을 점유할 수 있다.
- 조치:
  - owner 권한과 실제 식당 소유권을 upload endpoint에서 확인한다.
  - 계정/IP/식당별 일일 용량·횟수 quota와 전역 FFmpeg semaphore/worker queue를 둔다.
  - FFmpeg를 격리 worker로 옮기고 전역 동시 실행 수를 제한한다.
  - outbound overall deadline, bounded pool, bulkhead, circuit breaker를 적용하고 외부 호출을 DB transaction 밖으로 이동한다.
  - 공개 social auth에도 분산 rate limit을 둔다.
- 완료 기준: 외부 호출 pool 포화, worker queue 포화, 100MB 동시 요청 부하에서 제한 시간 내 실패하고 서비스 readiness가 유지된다.

### EVT-01 — 알림이 commit 후 메모리 큐에서 영구 유실 가능

- 확신도: 높음
- 근거:
  - [NotificationCommandService.java:278](../src/main/java/com/plateapp/plate_main/notification/service/NotificationCommandService.java#L278)은 transaction afterCommit에서 비동기 작업을 제출한다.
  - [AsyncConfig.java:15](../src/main/java/com/plateapp/plate_main/common/config/AsyncConfig.java#L15)은 최대 5 thread, queue 100의 비영속 실행기다.
  - queue reject, commit 직후 프로세스 종료, FCM 일시 장애를 복구하는 durable 상태가 없다.
- 영향: DB 이벤트는 성공했지만 사용자 알림이 조용히 사라진다.
- 조치:
  - push_outbox에 payload reference, idempotency key, attempts, nextAttemptAt, lease, status를 저장한다.
  - worker가 lease 후 전송하고 성공/재시도를 DB에 기록한다.
  - poison message DLQ와 운영 지표를 둔다.
- 완료 기준: queue 강제 포화, commit 직후 kill, FCM 5xx 후 재시도에서 한 번 이상 전달되고 중복 UI 효과가 없다.

### DEP-01 — 공식 보안 권고에 해당하는 해석 의존성

- 확신도: 버전 일치는 높음, 실제 악용성은 항목별 조건부
- 현재 주요 해석 버전: Spring Security 6.4.13, Spring Framework/WebMVC 6.2.14, Tomcat 10.1.49, PostgreSQL JDBC 42.7.8, Jackson Core 2.18.5, Netty 4.1.128.Final.
- 우선 확인 항목:

| 구성요소 | 권고 | 저장소 적용성 | 조치 |
|---|---|---|---|
| Spring Security 6.4.13 | [CVE-2026-22732](https://spring.io/security/cve-2026-22732/) CRITICAL, 6.4.0~6.4.14 영향 | servlet security 사용, 조건에 해당 | 지원되는 Boot/Security 라인으로 즉시 업데이트. 최소 fixed 6.4.15이나 6.4는 OSS 지원 종료이므로 지원 라인 선호 |
| Spring Framework 6.2.14 | [CVE-2026-22745](https://spring.io/security/cve-2026-22745/) MEDIUM | Windows 로컬 환경에서 filesystem /files 리소스 제공. Linux 운영이면 해당 전제 불충족 | 6.2.18 이상 또는 지원 Boot 라인. 로컬 file serving profile 제한 |
| Spring Boot 3.4.12 temp | [CVE-2026-40973](https://spring.io/security/cve-2026-40973/) HIGH | 로컬 공격자 + persistent servlet session이 전제. 현재 stateless이며 persistent session 설정 없음 | 직접 악용 가능성 낮음. 그래도 지원 Boot로 이동 |
| Tomcat 10.1.49 | [Apache Tomcat 10 보안 공지](https://tomcat.apache.org/security-10.html) | 10.1.54 이하 여러 수정 대상. 일부는 구성 조건부 | Boot BOM을 통해 현재 지원 Tomcat으로 업데이트하고 별도 pin 최소화 |
| pgjdbc 42.7.8 | [pgjdbc 42.7.11 release](https://github.com/pgjdbc/pgjdbc/releases/tag/REL42.7.11) | 악성·탈취 DB server가 과도한 SCRAM iteration을 줄 때 client DoS | 42.7.11 이상 |
| Netty 4.1.128 | [Netty 4.1.135 release](https://github.com/netty/netty/releases/tag/netty-4.1.135.Final) | Firebase 전이 의존성. Firebase 활성 시 사용 경로 확인 필요 | Firebase BOM/버전 업데이트 후 해석 Netty 확인 |

- 추가 결과:
  - CycloneDX SBOM을 OSV package-version API와 대조했을 때 52개 매치가 반환됐다. 중복 alias, 전이 모듈, 사용하지 않는 codec, 구성 전제 항목이 섞여 있으므로 52개를 곧바로 취약점 수로 해석하면 안 된다.
  - OWASP Dependency-Check는 NVD HTTP 429와 NoDataException으로 완료되지 않았다. 이는 CVE 0건이 아니라 해당 스캐너의 결론 없음이다.
- 조치 순서:
  1. Spring Boot 3.4는 기준일 현재 OSS 지원 라인이 아니므로 3.5.15 이상 또는 현재 지원되는 더 최신 안정 minor를 별도 PR에서 검증한다.
  2. dependency:tree로 Security, Framework, Tomcat, mail, Jackson, pgjdbc, Netty의 실제 해석 버전을 고정 검증한다.
  3. 전체 테스트와 PostgreSQL 통합 테스트, 인증 헤더·메일·multipart·Firebase 회귀를 수행한다.
  4. CI에 SBOM과 SCA를 필수화하고 suppression에는 근거와 만료일을 둔다.
- 완료 기준: 공식 fixed 범위를 충족하고, 남은 매치는 도달 가능성·구성·owner·재검토일이 기록되어 있다.

## 6. P2 발견사항

### P2 보안·개인정보

| ID | 문제와 근거 | 영향 | 권고 및 완료 기준 |
|---|---|---|---|
| SEC-07 | 일반 가입은 이메일 소유권 확인 없이 생성되고, 소셜 가입은 공급자 이메일과 무관한 client email을 허용한다. 아이디 찾기 응답도 존재 여부에 따라 다르다. | 사칭 가입, 계정 열거 | 모든 가입에서 서버 검증 이메일 증명 요구. find-id는 동일 응답·유사 지연. 계약 테스트 |
| SEC-08 | [BusinessNumberCrypto.java](../src/main/java/com/plateapp/plate_main/admin/storeapproval/service/BusinessNumberCrypto.java)는 별도 키가 없으면 JWT secret을 암호키로 쓰고 10자리 번호를 일반 SHA-256으로 검색한다. | DB 유출 시 오프라인 열거, JWT 키 회전과 데이터 복호화 결합 | KMS/envelope encryption, key version, 별도 HMAC 검색키. 키 회전 rehearsal |
| SEC-09 | SecurityConfig 86~90의 QnA, feedback, 추천·노출·식당 이벤트 쓰기가 공개이며 분산 rate limit/idempotency/quota가 없다. QnA 본문 제한도 부족하다. | spam, 비용·DB 증가, 추천 조작 | endpoint별 size, schema, rate, CAPTCHA/abuse score, idempotency, retention. 부하·남용 테스트 |
| SEC-10 | 점주 증빙은 Content-Type이 없으면 허용하고 size, magic byte, AV/CDR가 없다. 관리자에게 inline PDF를 제공한다. | 악성 문서, image bomb, 저장비용 | 10MB 등 별도 한도, quarantine, magic 검사, AV/CDR, attachment 기본. scan 전 접근 거부 |
| SEC-11 | CorsPreflightFilter가 OPTIONS를 먼저 끝내고 wildcard 시 origin 및 요청 header를 반사할 수 있다. | 위험한 환경변수 조합에서 credentialed cross-origin 허용 | custom filter 제거, Spring CORS 단일화, wildcard+credentials 조합 시작 실패. 구성 테스트 |
| SEC-12 | refresh 회전에 row lock/version이 없고 동시 재사용 시 둘 다 access token을 발급할 수 있다. 로그인은 deviceId를 저장하면서 먼저 deleteByUsername하여 사실상 단일 기기만 유지한다. plaintext legacy lookup도 남아 있다. | replay 창, 불명확한 multi-device 정책 | refresh family와 rotation counter, pessimistic/atomic consume, reuse detection. device별 세션 정책 명시 |

### P2 애플리케이션 정확성·계약

| ID | 문제와 근거 | 영향 | 권고 및 완료 기준 |
|---|---|---|---|
| APP-03 | [GlobalExceptionHandler.java:158](../src/main/java/com/plateapp/plate_main/common/error/GlobalExceptionHandler.java#L158)는 IllegalArgumentException, NoSuchElementException, SecurityException 다수를 500으로 보낸다. | 클라이언트 재시도·장애 지표 왜곡 | 도메인 AppException으로 400/403/404/409 통일. endpoint 오류 계약 테스트 |
| APP-04 | 추천 이벤트 한 건마다 전체 feature를 동기 재계산하고 홈 read도 feature/serving log를 쓴다. | 트래픽이 DB write·집계 부하로 증폭 | 이벤트 append-only, 비동기 증분 집계, read-only serving. 요청당 SQL과 p95 부하 기준 |
| APP-05 | 여러 서비스가 offset/limit을 PageRequest.of(offset/limit, limit)으로 변환한다. 비배수 offset은 잘못된 시작점을 반환한다. 홈 cursor는 깊이 제한 없이 조회량을 키운다. | 누락·중복·DoS성 deep pagination | page/size 또는 keyset cursor로 계약 통일, 서명·최대 깊이. 경계·페이지 연속성 테스트 |
| APP-06 | HomeContentFeed 검색은 최근 후보 일부만 메모리 필터하고 surface/lat/lng/radius 일부를 무시한다. | 오래된 결과 누락, API 계약 위반 | DB 검색·공간 조건 구현 또는 필드 제거와 versioning. 오래된 콘텐츠/반경 테스트 |
| APP-08 | 시청, 댓글, 좋아요, 친구 목록에 item별 사용자·카운트 조회 N+1이 있다. | 목록 크기에 비례한 지연·DB 부하 | projection/join 또는 batch map. 20개 목록의 쿼리 수 상수 검증 |
| APP-09 | 사업 신청, 추천 이벤트, block, like의 exists→insert/toggle과 클라이언트 제공 watch session ID에 DB unique/upsert 보장이 부족하다. | 동시 중복, PK 오류, 비결정 toggle | unique/partial unique, ON CONFLICT, idempotent PUT/DELETE. barrier 동시성 테스트 |
| APP-10 | fp_440, fp_460, fp_470, fp_100 등 같은 테이블을 복수 entity가 다르게 매핑한다. deleted_at 타입도 불일치한다. | persistence identity·날짜 정밀도·validate 위험 | 테이블당 canonical entity/repository 하나. PostgreSQL Hibernate validate |
| APP-11 | 댓글 생성은 대상 존재를 충분히 확인하지 않고 레거시 수정·삭제는 path target과 실제 소속을 비교하지 않는다. | orphan·다른 대상 경유 수정 | FK/활성 대상 확인, commentId+targetId 조건. 레거시 제거 |
| APP-12 | MapNearby가 store: group ID를 반환할 수 있지만 같은 ID를 입력하면 빈 결과를 반환한다. | 응답 cursor/filter 왕복 계약 파손 | store key 조회 구현 또는 형식 제거. 모든 반환 group ID round-trip 테스트 |
| APP-13 | Recipe GET 상세가 view count 쓰기를 수행하고 미존재 NoSuchElementException은 500이 된다. | GET 재시도 부작용, 잘못된 상태 | 404 도메인 예외, impression 이벤트 분리·중복 억제 |
| APP-15 | 서로 다른 ApiResponse 클래스, bare DTO, Map, ResponseEntity가 한 API 안에서 혼재한다. | 클라이언트·챗봇 수정 시 계약 파손 | 단일 versioned envelope/error schema, OpenAPI snapshot/consumer contract |
| APP-16 | 신고는 대상 존재, target ID와 username 일치, 자기 신고, 중복 정책을 검증하지 않는다. | moderation 데이터 오염 | target resolver, canonical FK/reference, 중복 정책과 idempotency |

### P2 데이터베이스

| ID | 문제와 근거 | 영향 | 권고 및 완료 기준 |
|---|---|---|---|
| DB-04 | V8/V9는 pg_constraint 이름을 schema/table/definition 없이 전역 조회하고, public 존재 확인 뒤 unqualified ALTER를 수행한다. FK 전 cleanup, index 전 대량 backfill, lock/statement timeout도 없다. | search_path 오작동, 배포 lock, 오염 데이터로 migration 실패 | schema-qualified DDL, pg_constraint 대상 검증, FK NOT VALID→cleanup→VALIDATE, timeout과 사전 dry-run |
| DB-05 | business_number_hash는 일반 index뿐이고 활성 신청 unique가 없다. place/social/push/recommendation event 등에도 핵심 unique/FK가 부족하다. | check-then-insert 경쟁·orphan | 데이터 감사 후 partial unique/FK 추가, 제약 위반을 409로 매핑 |
| DB-06 | RestaurantAdminService update는 menu/media/category를 모두 삭제·재생성한다. analytics는 menu_id를 저장하지만 FK/snapshot이 없다. | 설명 수정만으로 analytics 참조 무효화 | stable child ID, diff/upsert, soft delete, analytics snapshot 또는 FK 정책 |
| DB-07 | restaurants hard delete는 분석을 cascade 삭제하고 영상·피드는 set null한다. event/audit/outbox의 보존·partition 정책이 없다. | 감사·분석 손실, 무제한 테이블 성장 | 법적·제품 요구에 따른 soft delete/archive, retention/partition, purge index |
| DB-08 | rollback 문서와 post-deploy SQL이 현재 migration과 어긋나고 자동 실행되지 않는다. | 장애 시 잘못된 복구 | migration별 forward-fix/rollback 가능성, 검증 SQL을 CI·배포 gate에 포함 |

### P2 운영·테스트

| ID | 문제와 근거 | 영향 | 권고 및 완료 기준 |
|---|---|---|---|
| OPS-02 | application.yaml은 localhost DB, postgres user, 빈 password 기본값이고 deploy 필수 env는 JWT_SECRET뿐이다. 운영 file path도 Windows 고정값이다. | 오구성, 잘못된 환경 연결, Linux 경로 이상 | typed ConfigurationProperties + validation, prod profile에 안전한 기본값 없음, 필수 env fail-fast |
| OPS-03 | spring.jpa.open-in-view를 운영에서 끄지 않아 기본 true고 Hikari timeout/pool과 forwarding 전략이 명시되지 않았다. | 숨은 lazy query, 긴 connection 점유, 프록시 헤더 오해석 | OSIV false 후 query 정리, 부하 기반 pool/timeouts, trusted proxy 설정 |
| OPS-04 | systemd resource limit이 없고 in-place stop/start, root 배포 hook, ApplicationStop 실패 무시, 자동 rollback 부재가 남아 있다. | 단일 인스턴스 downtime, 권한 확대, 실패 배포 | root-owned read-only artifact, CPU/memory/file limit, rolling/blue-green, 실패 시 자동 rollback |
| QA-01 | 전체 커버리지가 낮고 친구·댓글·추천 등 핵심 서비스의 검증이 특히 부족하다. 현재 최소 전역 gate는 추가 하락만 막으며 변경 라인 기준은 없다. | P1 회귀를 테스트가 탐지하지 못함 | 우선 P1 동작 테스트, 변경 production line 80%, 보안·금융성 경로 branch 80%, 전역 임계값 점진 상향 |
| QA-02 | 테스트 profile은 H2, Flyway off라 PostgreSQL JSONB, partial index, ON CONFLICT, lock, migration을 검증하지 않는다. | 운영 전용 문법·제약 실패 은닉 | Testcontainers PostgreSQL, empty migrate/validate, concurrency integration |

## 7. P3 구조·관측·개발생산성

| ID | 문제 | 권고 |
|---|---|---|
| ARC-01 | OwnerStoreAnalyticsService, ProfileActivityDetailService, StoreApprovalService, SocialAuthService, HomeVideoService가 700~900줄대로 도메인·I/O·응답 조립을 함께 담당한다. | P1 테스트 확보 후 application/domain/adapter 경계로 분해. 외부 호출과 transaction 경계 분리 |
| OBS-01 | 이메일, IP, device ID/model, user-agent, social raw profile 등 개인정보를 저장하며 일부 로그·테이블에는 retention이 없다. | 목적별 보존기간, 마스킹/가명화, 접근권한, 삭제 job과 지표 |
| DX-01 | 표준 README, 로컬 실행, schema bootstrap, 장애대응·복구 runbook이 부족하고 문서 SQL이 분산돼 있다. | README + ADR + 운영 runbook + 데이터 사전. canonical 명령 한 곳 유지 |
| CI-01 | CI에 SCA, SAST, secret scan과 Dependabot/Renovate가 없고 생성한 SBOM을 취약점 판정 gate로 사용하지 않는다. | 신뢰 가능한 SCA와 suppression 만료 정책, secret/SAST scan, dependency update bot |
| PERF-01 | admin restaurant 목록 N+1, 지도 full aggregate, ILIKE %keyword%, owner analytics 다중 aggregate가 보인다. | 추측 index 추가보다 pg_stat_statements와 EXPLAIN ANALYZE로 상위 쿼리를 측정하고 trgm/projection/cache를 검증 |

## 8. 챗봇 조치용 작업 카드

아래 번호는 권장 merge 순서다. 1~3번은 서로 독립된 긴급 lane으로 동시에 시작할 수 있고, 7번의 운영 DB 감사도 읽기 전용 lane으로 병행할 수 있다. 각 카드는 한 PR을 기본으로 하며, 서로 다른 데이터 migration과 애플리케이션 변경을 무리하게 한 PR에 합치지 않는다.

| 순서 | 작업 카드 | 포함 ID | 선행조건 | 산출물 | 필수 검증 |
|---:|---|---|---|---|---|
| 1 | AUTH-BOUNDARY-01 | SEC-04, SEC-05 | 없음 | 추천 주체 강제, profile visibility policy | BOLA/비공개 부정 테스트 |
| 2 | OWNER-VERIFY-01 | BUS-01 | 없음 | server-owned verification proof | forged payload 통합 테스트 |
| 3 | PLATFORM-UPGRADE-01 | DEP-01 | 현재 지원 Boot target 결정 | BOM/Boot upgrade | Java 17 package, auth/mail/web regression |
| 4 | ACCOUNT-RECOVERY-01 | SEC-01~03, SEC-07, SEC-12 | Redis 또는 공유 limiter 결정 | one-time OTP, pending email, session families | reuse/XFF/enumeration/rotation/token invalidation |
| 5 | ADMIN-RBAC-01 | SEC-06 | ACCOUNT-RECOVERY-01의 tokenVersion helper 재사용 | role enum, super-admin guard, audit | 자기상승/강등 토큰 테스트 |
| 6 | FRIEND-STATE-01 | APP-01 | fp_150 데이터 감사 | canonical request state machine | A/B/동시 요청 통합 테스트 |
| 7 | DB-MAPPING-AUDIT-01 | DB-01 | 운영 read-only 접근 승인 | V8 감사 CSV와 correction table 초안 | 2인 검토, mutation 없음 |
| 8 | DB-MAPPING-FIX-01 | DB-01 | 감사 승인·백업 | V13 이상 forward fix | 전후 수량, sample, rollback plan |
| 9 | OBJECT-LIFECYCLE-01 | INT-01, SEC-10 | S3 lifecycle/IAM 확인 | staging + outbox/GC | fault injection |
| 10 | MEDIA-BULKHEAD-01 | RES-01 | worker topology 결정 | quota, queue, isolation | saturation/worker failure |
| 11 | PUSH-OUTBOX-01 | EVT-01 | delivery SLA 결정 | durable outbox worker | kill/retry/idempotency |
| 12 | DB-BASELINE-01 | DB-02, QA-02 | 운영 schema export 승인 | canonical Flyway baseline | empty PostgreSQL boot |
| 13 | SAFE-OPS-01 | SEC-11, OPS-02~04 | 운영 CORS·proxy·배포 정책 결정 | safe config, resource limits, rolling/rollback | 위험 CORS 시작 실패, rollback drill |
| 14 | DATA-INVARIANTS-01 | APP-09, DB-04~08 | duplicate/orphan 감사 | unique/FK/retention migrations | concurrency + validate |
| 15 | API-CORRECTNESS-01 | APP-03, APP-05~06, APP-10~13, APP-15~16 | 계약 versioning 결정 | errors/pagination/contracts | OpenAPI snapshot |
| 16 | QUALITY-GATE-01 | QA-01, CI-01 | P1 테스트 병합 | changed-line coverage, SCA/SAST/secret gates | clean runner |
| 17 | INGEST-PERFORMANCE-01 | SEC-09, APP-04, APP-08, PERF-01 | 운영 abuse/query metrics | quota·비동기 수집·측정 기반 최적화 | 남용 제한, query count/p95 전후 비교 |
| 18 | BUSINESS-CRYPTO-01 | SEC-08 | KMS·키 보관·회전 정책 승인 | versioned encryption + HMAC 검색키 | 기존 데이터 재암호화·rollback rehearsal |
| 19 | ARCH-CLEANUP-01 | ARC-01, DX-01, OBS-01 | P1/P2 안정화 | 모듈 분해·runbook·retention | 기능 동등성 |

### 챗봇 실행 규칙

1. 작업 시작 전 해당 ID의 증거 파일과 현재 테스트를 다시 읽는다.
2. 보안 문제는 먼저 실패하는 회귀 테스트를 작성한다.
3. 운영 데이터 mutation, IAM, bucket policy, DNS, 배포 전환은 사용자 승인 없이 실행하지 않는다.
4. 이미 적용된 Flyway migration을 수정하지 않는다. 새 버전 migration과 forward-fix를 사용한다.
5. API breaking change가 필요하면 버전 또는 호환 기간을 명시한다.
6. 한 PR은 한 실패 모드와 명확한 rollback 단위를 유지한다.
7. 완료 보고에는 변경 파일, threat/bug scenario, 테스트 명령, 결과, 남은 위험, rollback 방법을 포함한다.
8. 테스트 통과만으로 완료 처리하지 말고 각 카드의 부정 테스트와 완료 기준을 확인한다.
9. dependency upgrade는 개별 transitive pin보다 Boot/BOM 정렬을 우선하고 dependency:tree 결과를 첨부한다.
10. 성능 변경은 전후 SQL 수, p95/p99, CPU/DB time 등 측정값 없이는 완료 처리하지 않는다.

## 9. 공통 검증 명령과 게이트

### Windows 로컬

현재 사용자 환경의 JAVA_HOME은 JDK 루트가 아니라 bin을 가리켜 최초 Maven wrapper가 실패했다. 명령 프로세스에서만 다음처럼 JDK 루트로 설정했다.

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -B -Ddebug=false clean verify
.\mvnw.cmd -B dependency:tree
~~~

프로젝트 target은 Java 17이므로 최종 병합 게이트는 Java 17 clean runner에서 수행한다.

### Linux/CI 기본 게이트

~~~bash
./mvnw -B -Ddebug=false clean verify
./mvnw -B dependency:tree
~~~

추가해야 할 게이트:

- PostgreSQL Testcontainers empty migration + Hibernate validate
- 보안 부정 테스트
- JaCoCo 변경 라인 기준
- 생성된 CycloneDX SBOM을 판정하는 신뢰 가능한 SCA 데이터 소스
- secret scan과 SAST
- OpenAPI schema snapshot

### 권장 초기 품질 임계값

현재 수치에서 전역 80%를 즉시 강제하면 형식적 테스트가 늘 수 있다. 다음처럼 ratchet을 권장한다.

1. 변경된 production line 80% 이상.
2. SEC/BUS/APP P1 대상 service의 branch 80% 이상.
3. 1차 안정화 종료 시 전역 line 35%, branch 20%로 상향.
4. 핵심 도메인별 mutation 또는 동등한 부정 테스트를 점진 추가.

## 10. V8 운영 감사 SQL

아래 SQL은 읽기 전용이다. 먼저 DB snapshot/backup 정책과 실행 계정을 확인한다. 결과를 CSV로 보관하고 식당 소유자 또는 신뢰 가능한 식별자로 검토한다.

### Flyway 적용 여부

~~~sql
select installed_rank, version, description, script, checksum, installed_on, success
from flyway_schema_history
where version in ('8', '9')
order by installed_rank;
~~~

### 숫자 ID 매핑 중 이름·주소가 일치하지 않는 의심 행

~~~sql
select
    s.store_id,
    s.restaurant_id,
    coalesce(nullif(trim(s.store_name), ''), nullif(trim(s.title), '')) as video_store_name,
    trim(s.address) as video_address,
    r.title as restaurant_title,
    r.address as restaurant_address
from fp_300 s
join restaurants r on r.id = s.restaurant_id
where s.store_id::bigint = s.restaurant_id
  and (
      lower(trim(coalesce(nullif(s.store_name, ''), s.title, '')))
          is distinct from lower(trim(coalesce(r.title, '')))
      or lower(trim(coalesce(s.address, '')))
          is distinct from lower(trim(coalesce(r.address, '')))
  )
order by s.store_id;
~~~

### 연결 분포와 orphan

~~~sql
select
    count(*) as total_fp300,
    count(*) filter (where restaurant_id is null) as unmapped,
    count(*) filter (where restaurant_id is not null) as mapped,
    count(*) filter (where restaurant_id = store_id::bigint) as numeric_equal
from fp_300;

select s.store_id, s.restaurant_id
from fp_300 s
left join restaurants r on r.id = s.restaurant_id
where s.restaurant_id is not null
  and r.id is null
order by s.store_id;
~~~

### 한 restaurant에 과도하게 연결된 영상 표본

~~~sql
select
    s.restaurant_id,
    r.title,
    count(*) as video_count,
    count(distinct lower(trim(coalesce(nullif(s.store_name, ''), s.title, '')))) as distinct_video_names
from fp_300 s
join restaurants r on r.id = s.restaurant_id
where s.restaurant_id is not null
group by s.restaurant_id, r.title
having count(distinct lower(trim(coalesce(nullif(s.store_name, ''), s.title, '')))) > 1
order by distinct_video_names desc, video_count desc;
~~~

### 전체 매핑의 현재 이름·주소 불일치

숫자 ID가 같은 행뿐 아니라 이름·주소 매칭 또는 이후 수동 연결로 들어간 모든 행을 점검한다. 식당 상호·주소가 정상 변경된 경우도 나오므로 자동 수정 목록이 아니라 검토 후보 목록이다.

~~~sql
select
    s.store_id,
    s.restaurant_id,
    (s.store_id::bigint = s.restaurant_id) as numeric_id_equal,
    coalesce(nullif(trim(s.store_name), ''), nullif(trim(s.title), '')) as video_store_name,
    trim(s.address) as video_address,
    r.title as restaurant_title,
    r.address as restaurant_address
from fp_300 s
join restaurants r on r.id = s.restaurant_id
where lower(trim(coalesce(nullif(s.store_name, ''), s.title, '')))
          is distinct from lower(trim(coalesce(r.title, '')))
   or lower(trim(coalesce(s.address, '')))
          is distinct from lower(trim(coalesce(r.address, '')))
order by numeric_id_equal desc, s.store_id;
~~~

### 이름·주소 후보가 0개 또는 복수인 행과 기존 연결 불일치

~~~sql
with candidate_sets as (
    select
        s.store_id,
        s.restaurant_id,
        count(r.id) as candidate_count,
        coalesce(
            array_agg(r.id order by r.id) filter (where r.id is not null),
            '{}'::bigint[]
        ) as candidate_ids
    from fp_300 s
    left join restaurants r
      on lower(trim(coalesce(nullif(s.store_name, ''), s.title, ''))) = lower(trim(r.title))
     and lower(trim(coalesce(s.address, ''))) = lower(trim(r.address))
    group by s.store_id, s.restaurant_id
)
select store_id, restaurant_id, candidate_count, candidate_ids
from candidate_sets
where candidate_count <> 1
   or restaurant_id is null
   or not (restaurant_id = any(candidate_ids))
order by candidate_count desc, store_id;
~~~

### place_id와 restaurant_id의 다대다 의심 연결

~~~sql
select
    trim(place_id) as place_id,
    count(distinct restaurant_id) as restaurant_count,
    array_agg(distinct restaurant_id order by restaurant_id) as restaurant_ids
from fp_300
where place_id is not null
  and trim(place_id) <> ''
  and restaurant_id is not null
group by trim(place_id)
having count(distinct restaurant_id) > 1
order by restaurant_count desc, place_id;

select
    restaurant_id,
    count(distinct trim(place_id)) as place_count,
    array_agg(distinct trim(place_id) order by trim(place_id)) as place_ids
from fp_300
where place_id is not null
  and trim(place_id) <> ''
  and restaurant_id is not null
group by restaurant_id
having count(distinct trim(place_id)) > 1
order by place_count desc, restaurant_id;
~~~

### 업로더와 활성 점주 관계 교차 검증

일반 사용자가 식당 영상을 올릴 수 있는 제품 정책이라면 false가 정상일 수 있다. 이 결과는 자동 오류 판정이 아니라 correction evidence의 보조 자료로만 사용한다.

~~~sql
select
    s.store_id,
    s.restaurant_id,
    s.username as uploader_username,
    u.user_id as uploader_user_id,
    exists (
        select 1
        from store_owners so
        where so.store_id = s.restaurant_id
          and so.user_id = u.user_id
          and so.revoked_at is null
    ) as uploader_is_active_owner
from fp_300 s
left join fp_100 u on u.username = s.username
where s.restaurant_id is not null
order by uploader_is_active_owner, s.restaurant_id, s.store_id;
~~~

### 수정 전 필수 절차

1. 결과 export 및 checksum 보관.
2. 신뢰 가능한 place_id, 사업자 신청/승인, 관리자 식당 ID, 소유자 관계와 교차 검증.
3. correction table에 old_restaurant_id, new_restaurant_id, evidence, reviewer, reviewed_at 기록.
4. 두 명 검토와 DB backup 확인.
5. V13 이상에서 correction table에 있는 행만 조건부 수정.
6. 수정 row count가 예상과 다르면 transaction rollback.
7. owner analytics와 콘텐츠 표본 smoke.

## 11. 운영 확인 체크리스트

저장소만으로 확정할 수 없어 운영 담당자가 답해야 하는 항목이다.

- [ ] ALB/Nginx가 외부 X-Forwarded-For, Forwarded를 제거·재작성하는가?
- [ ] 애플리케이션이 직접 인터넷에 노출되지 않고 신뢰 프록시 security group만 허용하는가?
- [ ] S3 public access block, bucket policy, object ACL, CORS, lifecycle가 어떻게 설정돼 있는가?
- [ ] 배포 artifact bucket과 사용자 콘텐츠 bucket/IAM 역할이 분리돼 있는가?
- [ ] DB SSL mode, 인증 방식, connection limit, backup/PITR, restore drill이 있는가?
- [ ] V8/V9가 어느 환경에 적용됐고 의심 매핑은 몇 건인가?
- [ ] 서비스가 단일 EC2인지 다중 인스턴스인지, 배포 중 허용 downtime은 얼마인가?
- [ ] disk, JVM heap, DB pool, queue, FCM 실패율, 5xx, p95/p99 알람이 있는가?
- [ ] 개인정보별 보존기간과 삭제 요청 처리 정책이 승인돼 있는가?
- [ ] Firebase 기능이 운영에서 활성화돼 Netty 경로가 실제 도달 가능한가?
- [ ] local /files 경로가 운영에서 필요한가? 필요 없다면 profile에서 완전히 비활성화했는가?

## 12. 잘된 점

다음 기반은 유지하면서 개선하는 것이 좋다.

- JWT secret 최소 길이 검증과 access/refresh token type 구분이 있다.
- 신규 refresh token은 SHA-256으로 저장한다. 다만 legacy plaintext fallback 제거가 남아 있다.
- 관리자 세부 permission 검사와 일부 감사 로그 구조가 있다.
- 사업자번호 암호화는 AES-GCM과 랜덤 IV를 사용한다. 키 분리와 searchable hash 보강이 필요하다.
- 점주 문서는 짧은 presigned URL 방식이고 NTS client에는 명시적 timeout이 있다.
- 피드백 contact 90일 purge가 구현돼 있다.
- JPA ddl-auto=validate와 Flyway naming validation이 설정돼 있다.
- GitHub 배포 흐름은 OIDC와 최소 permissions, 테스트된 SHA 기반 artifact 전달을 일부 갖췄다.
- request ID와 표준형 ApiResponse를 도입하려는 구조가 있다.
- 저장소에서 실제 운영 secret 값은 확인되지 않았다.

## 13. 최종 수용 조건

다음 조건 전에는 “서버 전반 개선 완료”로 표시하지 않는다.

1. SEC-01~06, BUS-01, APP-01의 권한 부정 테스트가 통과한다.
2. V8 운영 감사와 필요한 V13 이상 정정이 승인·검증된다.
3. 빈 PostgreSQL에서 Flyway만으로 schema가 생성되고 애플리케이션이 기동한다.
4. DB/S3 fault injection과 notification restart 테스트가 통과한다.
5. 지원되는 Spring Boot/BOM으로 이동하고 공식 보안 권고의 해석 버전을 충족한다.
6. 운영 DB 장애 smoke에서 readiness가 실패하고 배포가 자동 중단·롤백된다.
7. Java 17 clean verify, SCA, secret/SAST scan, changed-line coverage가 PR 필수 gate다.
8. 공개 write endpoint마다 인증 필요성, rate limit, size, idempotency, retention 정책이 문서화된다.
9. 개인정보 보존·삭제·마스킹과 private profile 정책이 제품/법무 관점에서 승인된다.
10. 남은 P2/P3마다 owner, 목표일, 검증 기준이 이슈 트래커에 등록된다.

---

이 보고서는 2026-07-15의 커밋 6a830e09에서 시작해 같은 날 백엔드 즉시 조치가 반영된 작업 트리를 재검토한 결과다. 이후 코드·스키마·운영 설정 변경 시 남은 ID의 근거와 우선순위를 다시 확인해야 한다.
