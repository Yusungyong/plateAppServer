# Plate 마이페이지 API Accepted 계약

- 문서 상태: `Accepted (Phase 1)`
- Phase 2 상태: `Design Baseline` — 구현·출시 계약이 아님
- 결정일: 2026-07-16
- 작성일: 2026-07-16
- 계약 당사자: 모바일 앱 프론트엔드 팀, 제품 담당, 백엔드 팀
- 결정 근거: [마이페이지 API 계약 결정 요청서](./mypage-api-frontend-decision-request-2026-07-16.md)에 대한 2026-07-16 모바일·제품 회신
- 구현 상태: `Backend Candidate Implemented / Rollout Disabled`
- 적용 대상: 신규 마이페이지 허브를 사용하는 차기 Android/iOS 앱

이 문서는 모바일·제품 측 회신을 반영한 외부 API 계약이다. Phase 1 구현과 모바일 DTO·contract test는 이 문서를 기준으로 한다. Phase 2 항목은 방향과 설계 착수 순서를 합의하기 위한 기준이며, 별도의 스키마 검토와 계약 승인 전에는 출시 계약으로 보지 않는다.

## 0. 백엔드 구현 진행 상태 — 2026-07-16

Phase 1 백엔드 후보 구현은 코드와 H2 테스트 환경에서 작성했다. 운영·공유 DB에는 접속하거나 migration을 실행하지 않았고, 커밋·배포도 하지 않았다.

구현된 항목은 다음과 같다.

- 인증 전용 `GET /api/my/hub`와 `previewLimit=0..6`, 평면 `ApiResponse`, `503 MY_HUB_FEATURE_DISABLED`
- 프로필·콘텐츠·좋아요·받은 좋아요·친구·수신 대기 요청 집계와 날짜-only preview
- 양방향 차단, 현재 viewer의 콘텐츠·작성자 신고, 공개 계정·친구 관계를 반영한 허브 visibility
- 사용자 신고의 `targetUsername`, `targetUserId`, legacy `targetId` 저장 형태를 모두 반영
- 인증 전용 `GET /api/videos/{videoStoreId}`와 소유자·친구·차단·신고 판정
- 비공개 영상의 5분 S3 presigned URL 코드와 소유 object key 검증
- 이미지 `open_yn` nullable migration 파일, 명시된 신규 `Y/N` 저장 및 legacy null 보존
- 일반 가입 신규 계정의 `isPrivate=false` 명시
- HTTP envelope·명시적 null·request ID, 집계 SQL, visibility repository, playback URL 단위·통합 테스트

다만 다음 세 flag는 기본값이 모두 `false`다.

| 환경 변수 | 기본값 | 활성화 전 조건 |
|---|---:|---|
| `MY_HUB_ENABLED` | `false` | 아래 모든 출시 gate와 모바일 최소 버전 확정 |
| `MY_HUB_IMAGE_VISIBILITY_READY` | `false` | 이미지 legacy 정책, 상세·홈·그룹·주변 조회의 공통 visibility 적용 |
| `PRIVATE_MEDIA_DELIVERY_READY` | `false` | S3 Public Access Block, bucket policy 및 CDN origin/behavior 검증 |

`PRIVATE_MEDIA_DELIVERY_READY=false`에서는 공개 영상만 새 단건 endpoint로 재생할 수 있고, 서명 URL이 필요한 비공개 영상·비공개 계정 영상은 fail-closed로 `VIDEO_404` 처리한다.

아직 출시를 막는 항목은 다음과 같다.

- 기존 이미지 상세·홈·그룹·주변 API가 새 `openYn`과 계정 privacy를 공통 적용하지 않아 우회 경로가 남음
- 기존 영상 홈·프로필 API가 비공개 계정 visibility와 private media delivery를 공통 적용하지 않아 우회 경로가 남음
- legacy `isPrivate=null`, 이미지 `openYn=null`의 제품·보안 정책과 backfill 범위 미승인
- `V13__add_image_feed_visibility.sql`은 작성만 했으며 공유 DB에 적용하지 않음
- 실제 PostgreSQL 실행 계획·부하·인덱스 검증 미실시
- 정확한 Android/iOS 최소 적용 버전과 모바일 contract test 미확정

따라서 현재 상태는 "백엔드 후보 코드 존재"이며 "운영 출시 완료"가 아니다. 특히 공유 DB를 사용하는 로컬 서버를 평소처럼 기동하면 기본 활성화된 Flyway가 V13을 자동 적용할 수 있으므로, migration 승인 전에는 H2 테스트만 사용하고 공유 DB 대상 서버 기동을 금지한다.

현재 확인된 Android `2.0.5`, iOS `4.2`는 회신 시점의 앱 버전이다. 신규 허브를 활성화할 정확한 최소 `versionName`과 `MARKETING_VERSION`은 배포 빌드 확정 후 이 문서의 rollout 부록에 추가한다. 그 전에는 운영 feature flag를 활성화하거나 기존 API를 폐기하지 않는다.

## 구현·출시 전제

응답 형태와 의미는 `Accepted`이지만, 다음 데이터·보안 항목은 운영 활성화 전 필수 gate다. 의존하지 않는 controller·DTO·조회·테스트 구현은 먼저 진행할 수 있다.

1. 이미지 공개 상태가 현재 DB에 저장되지 않으므로 visibility 원천, 신규 저장 컬럼 및 기존 `GET /api/image-feeds/{id}`를 포함한 읽기 정책을 승인한다.
2. 비공개 영상의 고정 공개 URL 우회를 막을 signed URL 또는 인증 media delivery와 만료 후 재조회 방식을 구현한다.
3. legacy 계정의 nullable `isPrivate` 해석을 제품·보안과 확정하고 boolean `NOT NULL` 데이터로 정리한다.
4. 모바일이 정확한 Android/iOS 최소 적용 버전을 전달한 뒤 rollout 조건을 확정한다.

이 gate를 해결하지 않은 임시 `0`, 공개 기본값 또는 영구 공개 URL로 contract test를 통과시켜서는 안 된다.

## 1. 확정 결정

| 결정 | 확정 내용 | 적용 단계 |
|---|---|---|
| FD-01 | 방문·지도 필드는 응답에서 제외 | Phase 1 |
| FD-02 | 평면 `ApiResponse` envelope 사용 | Phase 1 |
| FD-03 | 현재 접근 가능한 활성 콘텐츠 기준 좋아요 집계 | Phase 1 |
| FD-04 | 소유자가 볼 수 있는 활성 게시물 기준 콘텐츠 집계 | Phase 1 |
| FD-05 | 실제로 완료된 방문을 방문 이벤트로 정의 | Phase 2 |
| FD-06 | 동행 방문은 `VISIT_WITH_FRIENDS`로 표현 | Phase 2 |
| FD-07 | canonical `placeId`와 좌표가 모두 있는 장소만 지도에 포함 | Phase 2 |
| 지도 기본 type | `ALL` | Phase 2 |
| FD-08 | 서버가 nullable 문자열 enum `primaryAction` 반환 | Phase 1 |
| FD-09 | `availableSections`를 순서 없는 가용 섹션 집합으로 사용 | Phase 1 |
| FD-10 | 불투명 `contentId`와 타입별 이동 ID를 함께 제공 | Phase 1 |
| FD-11 | cursor 목록에는 `pageInfo`만 제공하고 `totalCount` 제외 | Phase 2 |
| FD-12 | 정확한 시각과 날짜-only 값을 분리 | Phase 1·2 |
| FD-13 | 상태별로 제한된 fallback만 허용 | Phase 1 |

Phase 1 응답에는 다음 필드와 enum을 아예 넣지 않는다. `0`, `null`, 빈 배열 같은 임시값으로도 만들지 않는다.

- `visitedPlaceCount`
- `recentVisits`
- `VISITED_PLACES`
- `FOOD_MAP`
- `FRIEND_ACTIVITY`
- `TASTE_PROFILE`

계약상 nullable인 필드는 JSON에 명시적인 `null`로 반환한다. 위 Phase 1 제외 필드처럼 아직 계약에 포함되지 않은 기능만 필드 자체를 생략한다.

## 2. 공통 HTTP·응답 계약

### 2.1 성공 envelope

신규 endpoint의 성공 응답은 HTTP `200`과 다음 평면 구조를 사용한다.

```json
{
  "success": true,
  "data": {},
  "requestId": "01J2W8V4Y7R6K3M5N9P0Q2S4T6",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

- `success`: 항상 `true`
- `data`: endpoint별 응답 객체
- `requestId`: 유효한 inbound `X-Request-Id`를 검증해 채택하거나 없으면 서버가 생성한 불투명 correlation ID이며, 응답 헤더 `X-Request-Id`와 동일
- `timestamp`: 서버가 envelope를 생성한 UTC 시각
- 성공 응답에는 `message`, `errorCode`를 넣지 않는다.

### 2.2 오류 envelope

오류 응답은 HTTP 상태와 다음 다섯 필드를 사용한다. 내부 예외, SQL, stack trace는 노출하지 않는다.

```json
{
  "success": false,
  "message": "요청 값이 올바르지 않습니다.",
  "errorCode": "COMMON_400",
  "requestId": "01J2W8V4Y7R6K3M5N9P0Q2S4T6",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

- `success`: 항상 `false`
- `message`: 사용자에게 그대로 노출할 필요가 없는 진단용 안전한 메시지
- `errorCode`: 앱 분기용 안정적인 코드
- `requestId`, `timestamp`: 성공 응답과 같은 의미
- 오류 응답에는 `data`를 넣지 않는다.

### 2.3 시각 의미

- `timestamp`: 해당 HTTP 응답을 만든 시각이다. 재시도할 때마다 달라질 수 있다.
- `data.generatedAt`: 허브 데이터 스냅샷 또는 캐시를 만든 시각이다. 같은 캐시를 반환하면 여러 응답에서 같을 수 있다.
- `Instant` 값은 UTC ISO-8601 형식인 `YYYY-MM-DDTHH:mm:ss[.SSS]Z`로 반환한다.
- 서버는 하나의 허브 스냅샷을 완성하지 못하면 부분 `data`를 성공으로 반환하지 않는다.

## 3. Phase 1 마이페이지 허브

### 3.1 요청

```http
GET /api/my/hub?previewLimit=3
Authorization: Bearer {accessToken}
```

| 항목 | 계약 |
|---|---|
| 인증 | 필수. Bearer token의 principal만 사용 |
| `previewLimit` | 선택, 기본값 `3`, 허용 범위 `0..6` |
| 사용자 파라미터 | 받지 않음. 다른 `username` 조회 불가 |
| 성공 | 데이터가 없어도 `200` |
| 잘못된 limit | `400 COMMON_400` |

`previewLimit`는 `recentContent`와 `recentLikes` 각각에 적용되는 최대 항목 수다. `0`이면 두 배열은 비어 있지만, 카운트와 `availableSections`는 전체 데이터 기준으로 계산한다.

### 3.2 데이터 구조

```text
data
├─ profile
│  ├─ username: string
│  ├─ displayName: string
│  ├─ profileImageUrl: string | null
│  ├─ activeRegion: string | null
│  └─ isPrivate: boolean
├─ counts
│  ├─ contentCount: integer
│  ├─ videoCount: integer
│  ├─ imageCount: integer
│  ├─ likedContentCount: integer
│  ├─ receivedLikeCount: integer
│  ├─ friendCount: integer
│  └─ pendingFriendRequestCount: integer
├─ availableSections: Section[]
├─ recentContent: ContentPreview[]
├─ recentLikes: ContentPreview[]
├─ primaryAction: PrimaryAction | null
└─ generatedAt: Instant
```

프로필 규칙은 다음과 같다.

- `username`은 인증 principal과 일치한다.
- nickname이 null 또는 공백이면 `displayName`에 `username`을 사용한다.
- 프로필 이미지가 없으면 `profileImageUrl`은 null이다.
- 활동 지역이 없거나 공백이면 `activeRegion`은 null이다.
- `isPrivate`는 null 없이 현재 계정 공개 상태를 반환한다. legacy null 정책 승인과 backfill·`NOT NULL` 보강 전에는 Phase 1을 운영 활성화하지 않으며 임의의 `false`로 위장하지 않는다.
- 이메일, 휴대폰, 소셜 로그인 제공자, 로그인 IP 등 허브에 필요 없는 개인정보는 포함하지 않는다.

### 3.3 카운트 의미

모든 카운트는 음수가 아닌 정수이며 한 응답 스냅샷 안에서 서로 일관되어야 한다.

| 필드 | 확정 의미 |
|---|---|
| `videoCount` | 삭제되지 않고 활성 상태이며 소유자가 현재 볼 수 있는 `videoStoreId` 기준 영상 게시물 수. 소유자 자신의 비공개 게시물 포함 |
| `imageCount` | 이미지 파일 수가 아니라 삭제되지 않고 활성 상태인 `imageFeedId` 기준 이미지 게시물 수. 소유자 자신의 비공개 게시물 포함 |
| `contentCount` | 항상 `videoCount + imageCount` |
| `likedContentCount` | 내가 현재 좋아요한 대상 중 현재 내가 볼 수 있는 고유 활성 콘텐츠 수 |
| `receivedLikeCount` | 다른 활성 사용자가 내 활성 콘텐츠에 남긴 현재 유효한 좋아요 수 |
| `friendCount` | 수락 완료 상태인 고유 친구 수 |
| `pendingFriendRequestCount` | 내가 수신한 현재 대기 상태의 고유 친구 요청 수 |

집계 시 다음 중복 제거 키를 사용한다.

- 좋아요한 콘텐츠: `(contentType, contentPrimaryKey)`
- 받은 좋아요: `(contentType, contentPrimaryKey, likerUsername)`
- 친구: `counterpartUsername`
- 수신 대기 요청: `(requesterUsername, recipientUsername, pendingStatus)`

`receivedLikeCount`에서는 자기 좋아요, 취소된 좋아요, 삭제·비활성 콘텐츠, 탈퇴·비활성 사용자, 양방향 차단 관계 및 현재 권한 정책상 유효하지 않은 관계를 제외한다. 같은 visibility 정책을 카운트와 미리보기 목록에 모두 적용한다.

Phase 1 visibility의 구현 기준은 다음과 같다.

- 활성 사용자는 `fp_100` 사용자 행이 존재하고 서비스에서 탈퇴·비활성으로 판정되지 않은 사용자다.
- 소유자는 자신의 활성 콘텐츠를 공개 여부와 관계없이 볼 수 있다.
- 소유자가 아닌 viewer는 콘텐츠 자체가 공개 상태이고, 공개 계정 작성자이거나 비공개 계정 작성자와 수락된 친구 관계일 때만 볼 수 있다.
- viewer와 작성자 중 어느 방향으로든 활성 차단 행이 있으면 볼 수 없다.
- 신고 제외는 다른 사용자의 신고 전체가 아니라 현재 viewer가 해당 콘텐츠 또는 작성자에 남긴 아직 철회되지 않은 유효 신고에 적용한다.
- 활성 영상은 `useYn=Y`, `deletedAt IS NULL`이고 재생 파일 식별자가 존재해야 한다. 이미지의 활성·공개 판정은 구현·출시 gate에서 승인한 저장 정책을 따른다.

### 3.4 `availableSections`

Phase 1에서 서버가 반환할 수 있는 값은 다음 두 개뿐이다.

- `RECENT_CONTENT`
- `LIKED_CONTENT`

배열은 표시 순서를 뜻하지 않는 집합이다. 앱이 화면 순서를 관리하고 알 수 없는 값은 무시한다.

- `RECENT_CONTENT`: 서버 rollout 설정상 출시된 섹션이고 `contentCount > 0`일 때 포함
- `LIKED_CONTENT`: 서버 rollout 설정상 출시된 섹션이고 `likedContentCount > 0`일 때 포함
- `previewLimit=0`이어도 위 count가 양수이면 해당 값을 포함
- 아직 출시하지 않았거나 실제 데이터가 없는 섹션은 포함하지 않음

서버는 클라이언트 버전을 추정하지 않는다. 정확한 최소 앱 버전이 정해지기 전에는 feature flag로 신규 허브 전체를 비활성 상태로 유지한다.

### 3.5 `primaryAction`

`primaryAction`은 객체가 아닌 nullable 문자열 enum이다. 문구, 아이콘, 화면 이동은 모바일이 관리한다.

- `EXPLORE_CONTENT`
- `SET_ACTIVE_REGION`
- `CREATE_CONTENT`
- `FIND_FRIENDS`
- `null`

Phase 1 우선순위는 위에서 먼저 만족한 하나만 선택한다.

1. `contentCount == 0 && likedContentCount == 0`이면 `EXPLORE_CONTENT`
2. 그 외 활동이 있고 `activeRegion == null`이면 `SET_ACTIVE_REGION`
3. `contentCount == 0`이면 `CREATE_CONTENT`
4. `friendCount == 0`이면 `FIND_FRIENDS`
5. 그 외에는 null

Phase 2에서 `visitedPlaceCount`가 정식 도입되면 1번의 활동 없음 조건에 방문 활동도 포함한다. 그 변경 전에는 Phase 1 조건을 조용히 바꾸지 않는다.

### 3.6 `ContentPreview`

| 필드 | 타입 | 규칙 |
|---|---|---|
| `contentType` | `VIDEO \| IMAGE` | 필수 |
| `contentId` | string | 필수, 타입 간 유일하고 수명 동안 안정적인 불투명 ID |
| `videoStoreId` | integer \| null | VIDEO일 때 필수, IMAGE일 때 null |
| `imageFeedId` | integer \| null | IMAGE일 때 필수, VIDEO일 때 null |
| `placeId` | string \| null | canonical 장소가 있을 때만 값 제공 |
| `title` | string \| null | 원본 제목, 없으면 매장명, 둘 다 없으면 null |
| `thumbnailUrl` | string \| null | 없으면 null |
| `store` | object \| null | 매장 관련 값이 하나라도 있을 때 객체, 모두 없으면 null |
| `author` | object | 필수 |
| `createdAt` | Instant \| null | 정확한 생성 시각 |
| `createdOn` | LocalDate \| null | 생성 날짜만 아는 경우 |
| `createdTimePrecision` | `EXACT \| DATE` | 필수 |
| `likedAt` | Instant \| null | `recentLikes`의 정확한 좋아요 시각 |
| `likedOn` | LocalDate \| null | 좋아요 날짜만 아는 경우 |
| `likedTimePrecision` | `EXACT \| DATE` \| null | `recentLikes`에서는 필수, `recentContent`에서는 null |

`store`와 `author`의 모양은 다음과 같다.

```json
{
  "store": {
    "placeId": "ChIJ-plate-01",
    "storeName": "플레이트 키친",
    "address": "서울 성동구 성수동",
    "latitude": 37.5445,
    "longitude": 127.0560
  },
  "author": {
    "username": "su12ng",
    "displayName": "접시",
    "profileImageUrl": null
  }
}
```

중첩 필드 계약은 다음과 같다.

| 필드 | 타입 | 규칙 |
|---|---|---|
| `store.placeId` | string \| null | canonical 장소 ID. 상위 `placeId`와 둘 다 값이 있으면 반드시 동일 |
| `store.storeName` | string \| null | legacy 매장명 허용 |
| `store.address` | string \| null | 없으면 null |
| `store.latitude` | number \| null | WGS84 위도, 좌표 쌍이 불완전하면 null |
| `store.longitude` | number \| null | WGS84 경도, 좌표 쌍이 불완전하면 null |
| `author.username` | string | 필수 |
| `author.displayName` | string | nickname이 없으면 username |
| `author.profileImageUrl` | string \| null | 없으면 null |

`store.latitude`와 `store.longitude`는 둘 다 값이 있거나 둘 다 null이어야 한다. 상위 `placeId`와 `store.placeId`를 서로 다른 값으로 반환하지 않는다.

`contentId`의 내부 접두사나 조합 규칙은 계약이 아니다. 앱은 이를 파싱하지 않고 서버에 받은 문자열 그대로 식별에만 사용한다. 전환 기간의 상세 이동은 `videoStoreId` 또는 `imageFeedId`를 사용한다.

### 3.7 날짜 정밀도와 정렬

정확한 시각과 날짜-only 값은 동시에 채우지 않는다.

```json
{
  "createdAt": null,
  "createdOn": "2026-07-15",
  "createdTimePrecision": "DATE"
}
```

- `EXACT`: `*At`에 UTC 시각, `*On`은 null
- `DATE`: `*At`은 null, `*On`에 `YYYY-MM-DD`
- 날짜-only 값에 임의의 자정·정오 또는 timezone을 만들지 않음
- 원본 시각이 없거나 저장 기준 timezone을 증명할 수 없는 값은 `EXACT`로 승격하지 않음

`recentContent`, `recentLikes`, Phase 2 활동과 지도에 아래 1~3번 달력 날짜·정밀도 규칙을 공통 적용한다.

1. 비교용 달력 날짜 내림차순. `EXACT`는 시각을 `Asia/Seoul` 날짜로 변환하고 `DATE`는 `*On`을 그대로 사용한다.
2. 같은 달력 날짜에서는 `EXACT`가 `DATE`보다 먼저 온다.
3. `EXACT`끼리는 실제 UTC 시각 내림차순이다.
4. `ContentPreview`의 최종 동률은 서버의 고정 타입 순위 `VIDEO` 다음 `IMAGE`, 그 뒤 원본 PK 내림차순으로 결정한다. Phase 2 활동과 지도는 8절의 안정 키를 사용한다.

`recentContent`는 생성 시각을, `recentLikes`는 좋아요 시각을 정렬 기준으로 쓴다. 정렬 기준 자체가 없는 손상된 legacy 항목은 임의 시각을 생성하지 않고 미리보기에서 제외하며 관측 지표로 기록한다.

좋아요를 취소한 뒤 다시 누른 경우 `recentLikes`의 시각은 최초 좋아요 행 생성 시각이 아니라 현재 좋아요가 다시 활성화된 시각이다. timezone 없는 legacy `LocalDateTime`은 저장 기준을 입증하기 전 `DATE`로 낮춰 반환하고, UTC `Instant` 또는 `timestamptz` 전환 이후 새로 기록된 값만 `EXACT`로 반환한다.

## 4. Phase 1 성공 예시

예시의 도메인과 ID는 설명용이다.

### 4.1 일반 사용자 — `previewLimit=1`

```json
{
  "success": true,
  "data": {
    "profile": {
      "username": "su12ng",
      "displayName": "접시",
      "profileImageUrl": "https://cdn.example.com/profiles/su12ng.jpg",
      "activeRegion": "서울 성동구",
      "isPrivate": false
    },
    "counts": {
      "contentCount": 12,
      "videoCount": 5,
      "imageCount": 7,
      "likedContentCount": 24,
      "receivedLikeCount": 36,
      "friendCount": 5,
      "pendingFriendRequestCount": 2
    },
    "availableSections": [
      "RECENT_CONTENT",
      "LIKED_CONTENT"
    ],
    "recentContent": [
      {
        "contentType": "VIDEO",
        "contentId": "cnt_01J2W7S9K2R4",
        "videoStoreId": 128,
        "imageFeedId": null,
        "placeId": "ChIJ-plate-01",
        "title": "성수동 파스타",
        "thumbnailUrl": "https://cdn.example.com/videos/128-thumb.jpg",
        "store": {
          "placeId": "ChIJ-plate-01",
          "storeName": "플레이트 키친",
          "address": "서울 성동구 성수동",
          "latitude": 37.5445,
          "longitude": 127.0560
        },
        "author": {
          "username": "su12ng",
          "displayName": "접시",
          "profileImageUrl": "https://cdn.example.com/profiles/su12ng.jpg"
        },
        "createdAt": null,
        "createdOn": "2026-07-15",
        "createdTimePrecision": "DATE",
        "likedAt": null,
        "likedOn": null,
        "likedTimePrecision": null
      }
    ],
    "recentLikes": [
      {
        "contentType": "IMAGE",
        "contentId": "cnt_01J2W6R3P8T1",
        "videoStoreId": null,
        "imageFeedId": 456,
        "placeId": "ChIJ-plate-02",
        "title": "오늘의 디저트",
        "thumbnailUrl": "https://cdn.example.com/images/456-thumb.jpg",
        "store": {
          "placeId": "ChIJ-plate-02",
          "storeName": "카페 접시",
          "address": "서울 성동구 서울숲길",
          "latitude": 37.5460,
          "longitude": 127.0438
        },
        "author": {
          "username": "friend1",
          "displayName": "친구",
          "profileImageUrl": null
        },
        "createdAt": "2026-07-14T03:00:00Z",
        "createdOn": null,
        "createdTimePrecision": "EXACT",
        "likedAt": "2026-07-15T04:00:00Z",
        "likedOn": null,
        "likedTimePrecision": "EXACT"
      }
    ],
    "primaryAction": null,
    "generatedAt": "2026-07-16T08:59:59Z"
  },
  "requestId": "01J2W8V4Y7R6K3M5N9P0Q2S4T6",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

### 4.2 활동이 전혀 없는 사용자

```json
{
  "success": true,
  "data": {
    "profile": {
      "username": "new-user",
      "displayName": "new-user",
      "profileImageUrl": null,
      "activeRegion": null,
      "isPrivate": false
    },
    "counts": {
      "contentCount": 0,
      "videoCount": 0,
      "imageCount": 0,
      "likedContentCount": 0,
      "receivedLikeCount": 0,
      "friendCount": 0,
      "pendingFriendRequestCount": 0
    },
    "availableSections": [],
    "recentContent": [],
    "recentLikes": [],
    "primaryAction": "EXPLORE_CONTENT",
    "generatedAt": "2026-07-16T09:00:00Z"
  },
  "requestId": "01J2W8W12V7F",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

### 4.3 데이터가 있는 사용자 — `previewLimit=0`

배열은 비어 있어도 count를 기준으로 두 섹션을 유지한다.

```json
{
  "success": true,
  "data": {
    "profile": {
      "username": "su12ng",
      "displayName": "접시",
      "profileImageUrl": null,
      "activeRegion": "서울 성동구",
      "isPrivate": false
    },
    "counts": {
      "contentCount": 12,
      "videoCount": 5,
      "imageCount": 7,
      "likedContentCount": 24,
      "receivedLikeCount": 36,
      "friendCount": 5,
      "pendingFriendRequestCount": 2
    },
    "availableSections": [
      "RECENT_CONTENT",
      "LIKED_CONTENT"
    ],
    "recentContent": [],
    "recentLikes": [],
    "primaryAction": null,
    "generatedAt": "2026-07-16T09:00:00Z"
  },
  "requestId": "01J2W8X64M1A",
  "timestamp": "2026-07-16T09:00:01Z"
}
```

### 4.4 `placeId`·프로필 이미지·썸네일이 없는 VIDEO 미리보기

canonical 장소가 없어도 타입별 ID로 열 수 있어야 한다.

```json
{
  "success": true,
  "data": {
    "profile": {
      "username": "legacy-user",
      "displayName": "legacy-user",
      "profileImageUrl": null,
      "activeRegion": "서울 중구",
      "isPrivate": false
    },
    "counts": {
      "contentCount": 1,
      "videoCount": 1,
      "imageCount": 0,
      "likedContentCount": 0,
      "receivedLikeCount": 0,
      "friendCount": 1,
      "pendingFriendRequestCount": 0
    },
    "availableSections": [
      "RECENT_CONTENT"
    ],
    "recentContent": [
      {
        "contentType": "VIDEO",
        "contentId": "cnt_legacy_81",
        "videoStoreId": 81,
        "imageFeedId": null,
        "placeId": null,
        "title": "기존 매장명",
        "thumbnailUrl": null,
        "store": {
          "placeId": null,
          "storeName": "기존 매장명",
          "address": "기존 주소",
          "latitude": null,
          "longitude": null
        },
        "author": {
          "username": "legacy-user",
          "displayName": "legacy-user",
          "profileImageUrl": null
        },
        "createdAt": null,
        "createdOn": "2025-01-03",
        "createdTimePrecision": "DATE",
        "likedAt": null,
        "likedOn": null,
        "likedTimePrecision": null
      }
    ],
    "recentLikes": [],
    "primaryAction": null,
    "generatedAt": "2026-07-16T09:00:01Z"
  },
  "requestId": "01J2W8XNULL81",
  "timestamp": "2026-07-16T09:00:02Z"
}
```

## 5. Phase 1 영상 ID 단건 이동

FD-10의 전제조건은 기존 장소 기반 피드를 변경하지 않고 다음 단건 endpoint를 추가하는 것으로 확정한다.

```http
GET /api/videos/{videoStoreId}
Authorization: Bearer {accessToken}
```

이 endpoint를 선택한 이유는 현재 `/api/home/feed`가 장소 주변 목록용이며 `placeId`를 요구하고, 요청 영상이 후처리 필터에서 빠지면 다른 영상이 첫 항목이 될 수 있기 때문이다. 기존 `/api/home/feed`의 요청·응답 계약은 그대로 유지한다.

단건 조회 계약은 다음과 같다.

- 성공 시 `ApiResponse<VideoFeedItem>` 안에 정확히 한 객체를 반환한다.
- Bearer 인증은 필수다. token이 없거나 유효하지 않으면 `401 AUTH_401`, 만료되었으면 `401 AUTH_402`다.
- `placeId`가 없는 legacy 영상도 `videoStoreId`만으로 조회할 수 있다.
- 활성·미삭제이고 재생 파일이 존재하며 현재 viewer에게 열람 가능한 영상만 반환한다.
- 소유자는 본인의 활성 비공개 영상을 볼 수 있다.
- 존재하지 않음, 삭제, 비활성, 재생 파일 없음, viewer와 작성자의 양방향 차단, viewer의 유효 신고 또는 열람 권한 없음은 모두 `404 VIDEO_404`로 동일하게 처리해 존재 여부를 노출하지 않는다. 다른 사용자의 신고만으로 전체 viewer에게 숨기지 않는다.
- `likedByMe`는 인증 viewer의 현재 좋아요 여부이며 항상 boolean이다.
- `fileName`은 앱이 내부 구조를 해석하지 않는 playback URL이다. 비공개 미디어는 권한 검사를 우회할 수 있는 영구 공개 URL로 반환하지 않고, signed URL 또는 인증 media endpoint를 사용한다. 만료된 URL은 같은 단건 endpoint를 다시 조회해 갱신한다.
- 이미지 상세 이동은 기존 `GET /api/image-feeds/{imageFeedId}`를 사용한다.

`VideoFeedItem` 필드 계약은 다음과 같다.

| 필드 | 타입 | 규칙 |
|---|---|---|
| `storeId` | integer | 요청한 `videoStoreId`와 동일, 필수 |
| `placeId` | string \| null | canonical 장소가 없으면 null |
| `title` | string \| null | 없으면 null |
| `storeName` | string \| null | legacy 매장명 허용 |
| `address` | string \| null | 없으면 null |
| `lat`, `lng` | number \| null | 좌표 쌍이 불완전하면 둘 다 null |
| `fileName` | string | 재생 가능한 opaque playback URL, 필수 |
| `thumbnail` | string \| null | 없으면 null |
| `videoDuration` | integer \| null | 초 단위, 알 수 없으면 null |
| `createdAt` | LocalDate \| null | 기존 호환 필드, `YYYY-MM-DD` |
| `commentCount` | integer | 음수가 아닌 현재 유효 댓글 수 |
| `profileImageUrl` | string \| null | 작성자 이미지가 없으면 null |
| `username` | string | 작성자 username, 필수 |
| `likeCount` | integer | 음수가 아닌 현재 유효 좋아요 수 |
| `likedByMe` | boolean | 인증 viewer의 현재 좋아요 여부 |

`placeId`, 썸네일, 프로필 이미지가 없는 성공 예시는 다음과 같다.

```json
{
  "success": true,
  "data": {
    "storeId": 81,
    "placeId": null,
    "title": "기존 영상",
    "storeName": "기존 매장명",
    "address": "기존 주소",
    "lat": null,
    "lng": null,
    "fileName": "https://media.example.com/videos/81/play",
    "thumbnail": null,
    "videoDuration": 18,
    "createdAt": "2025-01-03",
    "commentCount": 0,
    "profileImageUrl": null,
    "username": "legacy-user",
    "likeCount": 0,
    "likedByMe": false
  },
  "requestId": "01J2W8Y9K5D0",
  "timestamp": "2026-07-16T09:00:02Z"
}
```

이 endpoint의 `createdAt`은 기존 `VideoFeedItem` 호환 필드로 `LocalDate`다. 허브 `ContentPreview`의 정밀도 필드와 혼동하지 않는다. private media delivery가 구현되기 전에는 소유자 비공개 영상 이동 contract가 충족된 것으로 보지 않는다.

## 6. 오류와 fallback

### 6.1 상태·코드 표

| 상황 | HTTP | `errorCode` | 허브 fallback |
|---|---:|---|---|
| `previewLimit` 형식·범위 오류 | 400 | `COMMON_400` | 안 함 |
| 인증 없음 | 401 | `AUTH_401` | 토큰 갱신 정책 적용 후 실패 시 로그인 처리 |
| access token 만료 | 401 | `AUTH_402` | 공통 interceptor에서 갱신 후 신규 API 1회 재호출 |
| 권한 없음 | 403 | `AUTH_403` | 안 함 |
| 허브 route가 없는 구버전 서버 | 404 | `COMMON_404` 또는 body 없음 | 기존 API로 1회 fallback |
| 인증 principal 사용자 없음 | 404 | `USER_404` | 기존 API로 1회 fallback |
| 영상 단건 조회 불가 | 404 | `VIDEO_404` | 허브 fallback 대상 아님 |
| 요청 제한 | 429 | `COMMON_429` | 안 함 |
| 허브 feature flag 비활성 | 503 | `MY_HUB_FEATURE_DISABLED` | 기존 API로 1회 fallback |
| 예상하지 못한 서버 오류 | 500 | `COMMON_500` | 화면 진입당 기존 API로 최대 1회 fallback |

feature-disabled의 확정 상태는 `503 Service Unavailable`, 코드는 `MY_HUB_FEATURE_DISABLED`다. 백엔드는 feature flag에 `501`을 의도적으로 사용하지 않는다. 다만 앱은 전환기 서버의 `404`와 `501`도 회신 정책대로 fallback할 수 있다.

### 6.2 오류 예시

#### 400

```json
{
  "success": false,
  "message": "previewLimit는 0 이상 6 이하여야 합니다.",
  "errorCode": "COMMON_400",
  "requestId": "01J2W900400",
  "timestamp": "2026-07-16T09:01:00Z"
}
```

#### 401

```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "errorCode": "AUTH_401",
  "requestId": "01J2W900401",
  "timestamp": "2026-07-16T09:01:01Z"
}
```

만료 token이면 같은 HTTP `401`에 `AUTH_402`를 사용한다.

#### 403

```json
{
  "success": false,
  "message": "권한이 없습니다.",
  "errorCode": "AUTH_403",
  "requestId": "01J2W900403",
  "timestamp": "2026-07-16T09:01:03Z"
}
```

#### 404

```json
{
  "success": false,
  "message": "리소스를 찾을 수 없습니다.",
  "errorCode": "COMMON_404",
  "requestId": "01J2W900404",
  "timestamp": "2026-07-16T09:01:04Z"
}
```

#### 429

```json
{
  "success": false,
  "message": "요청이 너무 많습니다.",
  "errorCode": "COMMON_429",
  "requestId": "01J2W900429",
  "timestamp": "2026-07-16T09:01:29Z"
}
```

`Retry-After` 응답 헤더는 optional이다. 값이 있으면 앱은 그 대기 시간을 존중하며, 헤더 유무와 관계없이 429에서 기존 API로 fallback하지 않는다.

#### 500

```json
{
  "success": false,
  "message": "서버 오류가 발생했습니다.",
  "errorCode": "COMMON_500",
  "requestId": "01J2W900500",
  "timestamp": "2026-07-16T09:01:50Z"
}
```

#### Feature disabled

```http
HTTP/1.1 503 Service Unavailable
```

```json
{
  "success": false,
  "message": "마이페이지 허브 기능을 현재 사용할 수 없습니다.",
  "errorCode": "MY_HUB_FEATURE_DISABLED",
  "requestId": "01J2W900503",
  "timestamp": "2026-07-16T09:01:53Z"
}
```

### 6.3 모바일 fallback 규칙

이 규칙은 `/api/my/hub` 화면 진입에만 적용한다.

- `404`, `501`, `MY_HUB_FEATURE_DISABLED`: 기존 마이페이지 API로 1회 fallback
- `401`: 공통 인증 interceptor가 token 갱신 후 신규 API를 1회 재호출. 허브 코드가 별도 갱신하지 않음
- `400`, `403`, `429`: fallback하지 않고 해당 오류 처리
- `5xx`, timeout, 일시적 network 오류: 화면 진입당 기존 API fallback 최대 1회
- 요청 취소: fallback하지 않음
- 신규·기존 API를 무한 재호출하거나 상시 병렬 호출하지 않음
- 영상 단건의 `VIDEO_404`는 마이페이지 기존 API fallback 조건으로 해석하지 않음

## 7. 알 수 없는 enum과 additive change

모바일 처리 규칙은 다음과 같다.

- 알 수 없는 `availableSections` 값은 그 요소만 무시한다.
- 알 수 없는 `primaryAction`은 null과 동일하게 처리해 CTA를 표시하지 않는다.
- 알 수 없는 `contentType`의 미리보기는 해당 item만 건너뛴다.
- 알 수 없는 JSON 필드는 무시한다.
- 하나의 알 수 없는 enum 때문에 전체 허브 parsing을 실패시키지 않는다.

미래 서버 응답을 가정한 `previewLimit=0` 예시는 다음과 같다.

```json
{
  "success": true,
  "data": {
    "profile": {
      "username": "su12ng",
      "displayName": "접시",
      "profileImageUrl": null,
      "activeRegion": "서울 성동구",
      "isPrivate": false
    },
    "counts": {
      "contentCount": 1,
      "videoCount": 1,
      "imageCount": 0,
      "likedContentCount": 0,
      "receivedLikeCount": 0,
      "friendCount": 1,
      "pendingFriendRequestCount": 0
    },
    "availableSections": [
      "RECENT_CONTENT",
      "SAVED_COLLECTIONS"
    ],
    "recentContent": [],
    "recentLikes": [],
    "primaryAction": "REVIEW_FAVORITES",
    "generatedAt": "2026-07-16T09:02:00Z"
  },
  "requestId": "01J2W901ENUM",
  "timestamp": "2026-07-16T09:02:00Z"
}
```

이 예시에서 앱은 `SAVED_COLLECTIONS`와 `REVIEW_FAVORITES`만 무시하고 나머지 응답을 사용한다.

## 8. Phase 2 설계 기준

이 절은 프론트 회신에 포함된 방향과 cursor 예시 요청을 보존하기 위한 `Design Baseline`이다. Phase 1 앱이 호출하거나 contract test 대상으로 삼지 않는다. endpoint와 item 세부 필드는 Phase 2 데이터 모델 승인 후 별도 `Accepted` 문서에서 최종 확정한다.

### 8.1 활동 타임라인 기준

예정 endpoint:

```http
GET /api/my/activity?types=VIDEO_POST,IMAGE_POST,VISIT_WITH_FRIENDS&limit=20&cursor={opaqueCursor}
Authorization: Bearer {accessToken}
```

- cursor가 없는 요청이 첫 페이지다.
- `limit` 기본값 `20`, 예정 허용 범위 `1..50`
- `items`와 `pageInfo`만 제공하고 `totalCount`는 제공하지 않는다.
- cursor는 불투명하며 앱이 파싱·수정하지 않는다.
- cursor는 사용자, 최초 snapshot, types, limit 및 마지막 keyset에 결속한다.
- 변조되거나 다른 사용자·필터에 재사용된 cursor는 `400 MY_ACTIVITY_CURSOR_INVALID`다.
- 같은 cursor를 재호출하면 snapshot 범위 안에서 같은 정렬 경계를 사용한다.
- `occurredAt`, `occurredOn`, `timePrecision`은 3.7절 규칙을 따른다.
- 같은 정밀도와 시각까지 같은 활동의 최종 tie-breaker는 안정적이고 고유한 `activityId`다. keyset cursor에도 이 값을 포함한다.

첫 페이지 예시:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "activityId": "act_01J2W6R3P8T1",
        "type": "IMAGE_POST",
        "occurredAt": "2026-07-15T12:00:00Z",
        "occurredOn": null,
        "timePrecision": "EXACT",
        "content": {
          "contentType": "IMAGE",
          "contentId": "cnt_01J2W6R3P8T1",
          "videoStoreId": null,
          "imageFeedId": 456,
          "placeId": "ChIJ-plate-02"
        },
        "place": null,
        "friends": []
      },
      {
        "activityId": "act_legacy_video_128",
        "type": "VIDEO_POST",
        "occurredAt": null,
        "occurredOn": "2026-07-15",
        "timePrecision": "DATE",
        "content": {
          "contentType": "VIDEO",
          "contentId": "cnt_01J2W7S9K2R4",
          "videoStoreId": 128,
          "imageFeedId": null,
          "placeId": null
        },
        "place": null,
        "friends": []
      }
    ],
    "pageInfo": {
      "nextCursor": "eyJ2IjoxLCJvcGFxdWUiOiJ...signature",
      "hasNext": true
    }
  },
  "requestId": "01J2W902PAGE1",
  "timestamp": "2026-07-16T09:03:00Z"
}
```

같은 날짜에서 `EXACT` 항목이 `DATE` 항목보다 먼저 나온다.

다음 페이지 예시:

```http
GET /api/my/activity?types=VIDEO_POST,IMAGE_POST,VISIT_WITH_FRIENDS&limit=20&cursor=eyJ2IjoxLCJvcGFxdWUiOiJ...signature
```

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "activityId": "act_01J2VZZ9VISIT",
        "type": "VISIT_WITH_FRIENDS",
        "occurredAt": "2026-07-14T10:30:00Z",
        "occurredOn": null,
        "timePrecision": "EXACT",
        "content": null,
        "place": {
          "placeId": "ChIJ-plate-03",
          "storeName": "방문한 식당",
          "latitude": 37.5550,
          "longitude": 127.0410
        },
        "friends": [
          {
            "username": "friend1",
            "displayName": "친구"
          }
        ]
      }
    ],
    "pageInfo": {
      "nextCursor": null,
      "hasNext": false
    }
  },
  "requestId": "01J2W902PAGE2",
  "timestamp": "2026-07-16T09:03:01Z"
}
```

### 8.2 방문·지도 기준

- 방문은 사용자가 실제 완료한 방문 이벤트다. 미래 일정과 단순 콘텐츠 등록은 방문이 아니다.
- 동행 친구가 없는 개인 방문도 기록할 수 있다.
- 한 번의 방문은 동행 친구 수와 무관하게 `visitCount` 1이다.
- 동행 친구는 자신이 기록한 방문의 `VISIT_WITH_FRIENDS`에만 포함한다.
- 친구가 독립적으로 기록한 위치 활동은 이 계약으로 노출하지 않는다.
- 방문 memo는 반환하지 않는다.
- 기존 친구별 행을 하나의 방문으로 정확히 묶을 수 없는 경우 임의 병합하지 않는다.

예정 지도 endpoint의 기본 type은 `ALL`이다.

```http
GET /api/my/places?type=ALL
Authorization: Bearer {accessToken}
```

- 예정 type은 `ALL`, `POSTED`, `LIKED`, `VISITED`이며 파라미터를 생략하면 `ALL`이다.
- 선택된 type 필터를 먼저 적용한다.
- 그 결과 중 canonical `placeId`와 좌표가 모두 있는 고유 장소만 반환한다.
- `totalCount`는 최종 반환되는 고유 canonical 장소 수다.
- `excludedWithoutCoordinatesCount`는 type 필터 이후 좌표 부족으로 제외한 고유 canonical 장소 수다.
- `contentCount`는 그 장소에 사용자가 등록한 고유 활성 게시물 수다.
- `visitCount`는 그 장소의 고유 완료 방문 이벤트 수다.
- 마지막 활동 시각은 `lastActivityAt`, `lastActivityOn`, `lastActivityTimePrecision`으로 분리한다.
- 장소 목록은 3.7절의 날짜·정밀도 규칙으로 마지막 활동 내림차순 정렬하고, 최종 동률은 canonical `placeId` 오름차순으로 고정한다.

## 9. Phase 2 착수 시점과 예상 구현 순서

Phase 1 계약이 확정된 2026-07-16부터 Phase 2의 읽기 전용 데이터 실태 조사와 모델 설계를 시작할 수 있다. 운영 DB 변경 및 Phase 2 endpoint 구현 일정은 아래 1~3단계 결과와 별도 migration 승인 후 확정한다.

1. `fp_200` 방문 행, canonical 장소 연결, 좌표 누락, legacy 시각 및 이미지 공개 상태를 읽기 전용으로 조사
2. 방문 이벤트·방문 그룹 ID·참여자·canonical `placeId`·실제 방문 시각 스키마 확정
3. 기존 행의 마이그레이션 가능 범위, 제외 기준, `legacy` 상태 필요 여부 승인
4. 신규 방문 write path와 명시적 visibility 저장을 먼저 구현
5. 정확히 판별 가능한 데이터만 backfill하고 검증·복구 절차 마련
6. 비방문 활동 타임라인과 서명된 keyset cursor 구현
7. 방문 및 `VISIT_WITH_FRIENDS`를 타임라인에 통합
8. `ALL` 기본 개인 음식 지도와 제외 카운트 구현
9. visibility·차단·cursor·query count·인덱스·부하 contract test 후 단계적 rollout
10. `TASTE_PROFILE`은 위 데이터 신뢰성이 확보된 뒤 후속 단계에서 별도 설계

로컬과 운영이 같은 DB를 공유하는 현재 환경에서는 조사 쿼리를 읽기 전용으로 제한한다. migration, backfill, 대량 update와 테스트 데이터 생성은 로컬 검증이라는 이유만으로 실행하지 않으며 영향 분석·백업·복구 계획과 별도 승인을 거친다.

## 10. 이미지 공개 상태 migration gate

기존 이미지의 공개 범위를 일괄적으로 `Y`로 바꾸지 않는다. Phase 1의 이미지 count·미리보기와 Phase 2 지도 출시 전에 다음을 완료해야 한다.

1. 현재 스키마 값과 null 비율, 구버전 앱, 관리자 기능 및 모든 이미지 업로드·수정 경로 조사
2. 기존 앱이 실제로 적용하던 계정 공개 상태와 이미지 노출 규칙 확인
3. 신규 write에서 `openYn` 또는 후속 visibility 값을 명시적으로 저장
4. legacy null을 공개·비공개·별도 상태 중 어떻게 취급할지 제품·보안·백엔드 공동 승인
5. 대상 건수, 구버전 영향, rollback과 검증 SQL을 포함한 migration 계획 작성
6. 공통 visibility 정책을 count, preview, 상세 조회에 동일하게 적용하는 테스트 통과

원래 의도를 복원할 수 없는 행은 프론트 결정만으로 공개 전환하지 않는다. 정책 승인 전에는 데이터 공개 범위를 조용히 넓히는 migration을 실행하지 않는다.

## 11. 역할과 출시 gate

| 주체 | 책임 |
|---|---|
| 백엔드 | 이 계약의 endpoint·DTO·집계·visibility·오류 코드·단건 영상 API·테스트 구현 |
| 모바일 | nullable 필드, 알 수 없는 enum, `primaryAction`, fallback, `placeId` 없는 영상 이동 구현 및 정확한 최소 앱 버전 전달 |
| 제품·보안 담당 | legacy 계정·이미지 공개 정책과 Phase 2 legacy 방문 정책 승인 |
| 공동 | contract test fixture 검토, rollout·rollback 기준 확정 |

응답 shape를 다시 결정할 필요는 없지만 다음 운영 결정은 아직 남아 있다.

- 모바일·제품: 신규 허브가 포함되는 정확한 Android/iOS 최소 버전
- 제품·보안·백엔드: nullable `isPrivate`와 이미지 legacy visibility의 해석 및 migration 범위
- 백엔드·모바일: private playback URL 만료·재조회 contract test
- 제품·백엔드: Phase 2에서 정확히 묶을 수 없는 기존 방문의 제외 또는 `legacy` 표시 정책

백엔드 구현 시 기존 앱·관리자 계약을 보호하기 위해 다음 사항을 지킨다.

- 중첩 오류형 `common.dto.ApiResponse`가 아니라 평면형 `common.api.ApiResponse`를 신규 endpoint에 사용하고, 기존 공통 응답 클래스를 전역 변경하지 않는다.
- 현재 type mismatch·validation handler는 `COMMON_402` 또는 상세 `data`를 넣을 수 있다. `previewLimit`는 endpoint에서 문자열을 직접 parse·range 검증하거나 전용 exception mapping을 사용해 형식·범위 오류 모두 `COMMON_400`과 이 계약의 다섯 필드만 반환한다.
- 조회 실패에 일반 `IllegalArgumentException`을 사용하면 `500`이 될 수 있으므로 영상 단건은 `VIDEO_404`에 연결된 domain exception을 사용한다.
- 영상 단건 `GET`도 인증 필수로 유지하고, 같은 경로의 기존 `PATCH`·`DELETE` 인증을 약화시키는 public matcher를 추가하지 않는다.
- feature flag가 꺼져도 route는 등록한 채 요청 guard에서 `503 MY_HUB_FEATURE_DISABLED`를 반환한다. controller 자체를 조건부 미등록해 generic 404로 바꾸지 않는다.
- 허브 DTO 내부의 계약상 nullable 필드는 명시적 null로 직렬화되는지 확인한다.

Phase 1 운영 활성화 전에 최소한 다음을 검증한다.

- 일반 사용자, cold start, `previewLimit=0`, `previewLimit=1..6`
- `contentCount == videoCount + imageCount`
- count와 preview에 동일한 활성·삭제·차단·visibility 정책 적용
- 소유자 비공개 콘텐츠 포함과 타인 비공개 콘텐츠 차단
- `placeId` 없는 VIDEO의 단건 이동
- 프로필 이미지·썸네일·장소·좌표 null 직렬화
- `400`, `401`, `403`, `404`, `429`, `500`, feature-disabled envelope
- 허브 fallback 횟수와 token refresh 중복 방지
- 알 수 없는 enum과 additive JSON field에 대한 모바일 contract test
- 날짜-only 값에 가짜 시각이 만들어지지 않는지와 혼합 정밀도 정렬
- 기존 앱·관리자 endpoint의 요청·성공 응답에 회귀가 없는지
- N+1, query count, 인덱스 사용 및 응답 시간 기준
- `requestId` 헤더·body 일치와 민감정보 비노출

## 12. 변경 관리

- 기존 필드의 의미, nullability, enum 의미 또는 fallback 조건 변경은 additive change가 아니므로 프론트와 재승인한다.
- 새 JSON 필드와 새 enum 값은 기존 앱이 무시할 수 있다는 전제에서 additive change로 추가할 수 있다.
- Phase 1 제외 필드와 Phase 2 endpoint는 해당 단계의 새 `Accepted` 계약 후 추가한다.
- 이 문서는 목표 계약과 현재 백엔드 후보 구현 상태를 함께 기록한다. 양측 contract test와 모든 rollout gate 통과 전 상태는 계속 `Rollout Disabled`다.
