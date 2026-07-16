# Plate 마이페이지 API 계약 결정 요청서

- 문서 상태: `Answered`
- 작성일: 2026-07-16
- 회신일: 2026-07-16
- 수신: 모바일 앱 프론트엔드 팀
- 발신: 백엔드 팀
- 관련 문서: `Plate 마이페이지 백엔드 API 개편 요청서` (2026-07-16, Proposed)
- 확정 계약: [Plate 마이페이지 API Accepted 계약](./mypage-api-accepted-contract-2026-07-16.md)
- 목적: 마이페이지 API 구현 전에 앱에서 관찰되는 동작과 응답 계약을 모바일 팀이 최종 선택하도록 요청

> 모바일·제품 측 회신으로 FD-01~FD-13의 선택이 완료되었다. 이 문서는 결정 과정의 기록으로 보존하며, 구현과 contract test의 기준은 위 `Accepted` 계약을 사용한다.

## 1. 결정 권한과 회신 원칙

이 문서의 `FD-*` 항목은 모바일 프론트엔드 팀에 결정을 위임한다. 모바일 팀의 명시적인 회신을 마이페이지 신규 API의 계약 입력으로 사용한다.

모바일 팀이 제품 정책의 최종 결정 권한을 갖지 않은 항목은 기획·서비스 책임자와 내부 조율하되, 백엔드에는 충돌 없는 하나의 확정 회신으로 전달한다.

- 각 필수 항목에서 선택지 하나를 고른다.
- 권장안은 백엔드가 현재 데이터, 하위 호환성, 보안 및 구현 위험을 검토해 제시한 기본 제안이다.
- 권장안과 다른 선택도 가능하지만, 선택지에 적힌 일정·스키마·앱 처리 영향을 함께 수용해야 한다.
- 회신이 없는 항목은 백엔드가 임의로 추정하지 않으며 해당 기능 구현을 보류한다.
- 선택지에 없는 요구사항은 원하는 JSON 예시와 화면 동작을 함께 적는다.

모바일 팀이 최종 결정하는 범위는 필드 의미, 화면 노출 조건, null 처리, enum, fallback, 페이지 정보처럼 앱에서 관찰되는 동작이다. DB 스키마, 인덱스, 커서 서명, 트랜잭션, 캐시 구현은 확정된 외부 계약을 만족하는 범위에서 백엔드가 책임진다.

## 2. 선택과 무관하게 적용되는 서버 기준

아래 항목은 보안·데이터 무결성 기준이므로 선택 대상이 아니다.

1. `/api/my/*`는 인증된 토큰의 사용자를 기준으로 조회하며 요청에서 다른 `username`을 받지 않는다.
2. 이메일, 휴대폰, 로그인 제공자, 로그인 IP 및 내부 예외 정보는 마이페이지 응답에 포함하지 않는다.
3. 삭제·탈퇴·양방향 차단·비공개·콘텐츠 공개 상태는 페이지 제한을 적용하기 전에 서버 쿼리에서 필터링한다.
4. 계산할 수 없는 값은 정상 값처럼 `0`으로 위장하지 않는다.
5. 잘못되거나 변조된 cursor, limit 및 enum은 `400`으로 거절한다.
6. 비공개 콘텐츠는 권한이 확인된 사용자에게만 전달한다. 정적 공개 URL로 보호가 불가능한 경우 URL 전달 방식을 별도로 보강한다.
7. 신규 enum을 추가할 수 있도록 앱은 알 수 없는 enum을 무시해야 하며, 기존 필드의 의미는 조용히 변경하지 않는다.

## 3. 현재 백엔드에서 확인된 사실

| 항목 | 현재 상태 |
|---|---|
| 기존 프로필 `likeCount` | 내가 누른 활성 영상·이미지 좋아요 수의 합계 |
| `receivedLikeCount` | 현재 통합 집계 없음. 신규 쿼리 필요 |
| 영상 `storeId` | 장소 ID가 아니라 영상 콘텐츠 행의 PK |
| 이미지 `feedId` | 이미지 피드 콘텐츠 행의 PK |
| 기존 `visitedStoresCount` | 실제 방문 수가 아니라 영상 게시 수를 세고 있어 재사용 불가 |
| 현재 방문 데이터 | 친구 한 명당 한 행이며 방문 그룹 ID와 canonical `placeId`가 없음 |
| 이미지 `openYn` | 업로드 응답에는 있지만 현재 콘텐츠 행에 저장되지 않음 |
| 시간 데이터 | 영상은 날짜만 저장하고, 다른 활동은 timezone 없는 시각을 사용 |
| 응답 envelope | 주 API는 평면 오류 구조를 사용하고 일부 레거시 API만 중첩 오류 구조를 사용 |

## 4. 필수 결정 요청

### FD-01 — Phase 1 방문·지도 범위

현재 방문 데이터만으로는 `visitedPlaceCount`, `recentVisits`, `VISIT`, `FRIEND_VISIT` 및 `FOOD_MAP`을 정확하게 제공할 수 없다.

- [ ] **A. 방문·지도 필드를 Phase 1에서 제외한다. (권장)**
  - Phase 1은 프로필, 콘텐츠, 좋아요, 친구 카운트와 최근 콘텐츠·좋아요만 제공한다.
  - `visitedPlaceCount`, `recentVisits`, `FOOD_MAP` enum은 방문 모델 보강 후 additive field로 추가한다.
- [ ] **B. 방문 모델 보강이 끝날 때까지 Phase 1 허브 출시를 보류한다.**
  - 처음부터 제안서의 방문 필드까지 모두 제공한다.
  - 방문 스키마 마이그레이션과 기존 데이터 정리가 Phase 1 선행 작업이 된다.

`0`이나 빈 배열을 임시값으로 반환하는 선택지는 제공하지 않는다. 실제 데이터가 없는 상태와 서버가 계산할 수 없는 상태를 앱이 구분할 수 없기 때문이다.

### FD-02 — 공통 성공·오류 응답 형식

- [ ] **A. 현재 주 API의 평면 envelope를 사용한다. (권장)**

```json
{
  "success": false,
  "message": "잘못된 cursor입니다.",
  "errorCode": "MY_ACTIVITY_CURSOR_INVALID",
  "requestId": "server-request-id",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

  - 현재 인증 401/403 응답 및 대부분의 API와 같은 파서를 사용할 수 있다.
  - `traceId` 대신 현재 서버가 실제로 생성하는 `requestId`를 사용한다.

- [ ] **B. 중첩 `error` envelope를 신규 API 버전으로 도입한다.**

```json
{
  "success": false,
  "error": {
    "code": "MY_ACTIVITY_CURSOR_INVALID",
    "message": "잘못된 cursor입니다.",
    "retryable": false,
    "requestId": "server-request-id"
  }
}
```

  - `/api/v2/my/*`와 같이 버전을 분리하고 인증 401/403 처리까지 같은 구조로 변경한다.
  - 모바일은 신규 오류 파서를 추가해야 한다.

### FD-03 — 좋아요 카운트 의미

- [ ] **A. 현재 접근 가능한 활성 콘텐츠 기준으로 계산한다. (권장)**
  - `likedContentCount`: 내가 현재 좋아요한 콘텐츠 중 지금도 열람 가능한 콘텐츠 수
  - `receivedLikeCount`: 다른 사용자가 내 활성 콘텐츠에 남긴 현재 활성 좋아요 수
  - 삭제·차단·열람 불가 대상과 자기 좋아요는 `receivedLikeCount`에서 제외한다.
- [ ] **B. 콘텐츠 열람 가능 여부와 관계없이 활성 like 행을 센다.**
  - 기존 프로필 `likeCount`와 가장 유사하지만 카운트가 1 이상인데 목록이 비어 있을 수 있다.
- [ ] **C. 현재 활성 수와 누적 이력을 모두 제공한다.**
  - 별도의 `lifetimeReceivedLikeCount`와 좋아요 이벤트 이력이 필요하다.
  - 구현 및 데이터 보관 범위가 커진다.

### FD-04 — 내 콘텐츠 카운트 범위와 이미지 단위

- [ ] **A. 내가 볼 수 있는 활성 게시물을 센다. (권장)**
  - 삭제되지 않은 내 공개·비공개 게시물을 모두 포함한다.
  - `imageCount`는 이미지 파일 수가 아니라 `imageFeedId`를 가진 이미지 게시물 수다.
  - `contentCount = videoCount + imageCount`를 항상 만족한다.
- [ ] **B. 공개 상태인 게시물만 센다.**
  - 비공개 게시물은 내 마이페이지 카운트와 최근 콘텐츠에서도 숨긴다.
- [ ] **C. 이미지 파일 개수를 `imageCount`로 사용한다.**
  - 하나의 `imageFeedId`에 여러 이미지가 있어 `contentCount`와 배열 단위가 달라진다.

### FD-05 — ‘방문’의 제품 의미

이 항목은 FD-01에서 B를 선택하거나 Phase 2 방문 기능을 시작하기 전에 반드시 확정한다.

- [ ] **A. 사용자가 실제 방문 완료를 기록한 한 건을 방문 이벤트로 본다. (권장)**
  - 친구가 없는 단독 방문도 허용한다.
  - 미래 일정은 방문 수와 활동 타임라인에서 제외한다.
  - 한 번의 방문은 친구 수와 무관하게 `visitCount` 1이다.
- [ ] **B. 예정된 방문 일정도 방문 활동에 포함한다.**
  - 과거 방문과 미래 일정을 API에서 구분하는 상태 필드가 필요하다.
- [ ] **C. 콘텐츠를 게시하면 해당 장소를 방문한 것으로 간주한다.**
  - 별도 방문 입력은 필요 없지만 게시하지 않은 방문은 기록할 수 없다.

### FD-06 — 친구 방문 활동의 의미

- [ ] **A. `VISIT_WITH_FRIENDS`를 사용한다. (권장)**
  - 내가 기록한 방문에 동행한 친구 목록만 표시한다.
  - 친구가 독립적으로 기록한 위치 활동은 내 타임라인에 노출하지 않는다.
- [ ] **B. `FRIEND_VISIT`으로 친구가 기록한 방문도 내 타임라인에 표시한다.**
  - 현재 친구 관계, 동의, 차단, 친구 해제 후 과거 기록 노출 정책이 추가로 필요하다.
- [ ] **C. 초기 타임라인에서는 친구 관련 방문 유형을 제외한다.**

어떤 선택에서도 방문 memo는 친구 활동 응답에 포함하지 않는 것을 서버 기본 정책으로 한다.

### FD-07 — 개인 음식 지도 장소 기준

- [ ] **A. canonical `placeId`와 좌표가 모두 있는 장소만 반환한다. (권장)**
  - `totalCount`는 실제로 반환 가능한 고유 장소 수다.
  - `excludedWithoutCoordinatesCount`를 항상 숫자로 제공한다.
  - `contentCount`는 해당 장소에 내가 올린 고유 게시물 수, `visitCount`는 고유 방문 이벤트 수다.
- [ ] **B. `placeId`가 없으면 `storeName + address` 임시 키를 허용한다.**
  - 과거 데이터를 더 많이 보여줄 수 있지만 이름·주소 변경에 따라 중복되거나 다른 장소가 합쳐질 수 있다.
- [ ] **C. 장소 데이터 정리가 끝날 때까지 지도 API를 제공하지 않는다.**

추가 선택:

- `/api/my/places`의 기본 `type`: [ ] `ALL` (권장) / [ ] `POSTED` / [ ] `LIKED` / [ ] `VISITED`

### FD-08 — `primaryAction` 책임과 우선순위

- [ ] **A. 서버는 enum만 반환하고 모바일이 문구를 현지화한다. (권장)**
  - 제안 우선순위는 아래와 같다.
    1. `contentCount`, `likedContentCount` 및 제공 중인 방문 활동 카운트가 모두 0이면 `EXPLORE_CONTENT`
    2. 활동은 있지만 지역이 없으면 `SET_ACTIVE_REGION`
    3. 친구가 없으면 `FIND_FRIENDS`
    4. 등록 콘텐츠가 없으면 `CREATE_CONTENT`
    5. 그 외에는 `null`
- [ ] **B. `primaryAction`을 제거하고 모바일이 프로필·카운트로 직접 결정한다.**
- [ ] **C. 서버가 enum, title, description을 모두 반환한다.**
  - 앱 locale 전달 방법, 번역 책임 및 locale별 캐시 키가 추가로 필요하다.

A를 선택하면서 우선순위를 변경하려면 원하는 enum 순서를 회신에 적는다.

### FD-09 — `availableSections` 순서와 신규 enum 처리

- [ ] **A. 서버 배열은 순서가 없는 가용 섹션 집합으로 사용한다. (권장)**
  - 모바일이 화면 표시 순서를 관리한다.
  - 모바일은 모르는 enum을 무시한다.
  - 서버는 endpoint와 화면이 실제 출시된 섹션만 반환한다.
- [ ] **B. 서버 배열 순서를 화면 표시 순서로 사용한다.**
  - 화면 순서 변경이 서버 배포와 계약 변경에 결합된다.

FD-01에서 A를 선택하면 Phase 1의 후보는 `RECENT_CONTENT`, `LIKED_CONTENT`이며 `FOOD_MAP`, `FRIEND_ACTIVITY`, `TASTE_PROFILE`은 반환하지 않는다.

### FD-10 — 콘텐츠 식별자와 `placeId`가 없는 과거 데이터

- [ ] **A. 콘텐츠 타입별 ID만으로 상세 이동할 수 있게 한다. (권장)**
  - `contentId`는 앱이 파싱하지 않는 불투명 문자열로 취급한다.
  - VIDEO는 `videoStoreId` 필수, `imageFeedId` null이다.
  - IMAGE는 `imageFeedId` 필수, `videoStoreId` null이다.
  - `placeId`는 nullable이며 장소 화면 기능에만 사용한다.
- [ ] **B. `placeId`가 없는 콘텐츠는 허브·타임라인에서 제외한다.**
- [ ] **C. 모든 기존 데이터의 `placeId` 보정이 끝난 후 신규 API를 출시한다.**

공통 권장 null 규칙:

- `thumbnailUrl`, `placeId`, 좌표는 없으면 `null`
- `author.displayName`은 nickname이 없으면 username으로 대체
- `title`은 원본 title이 없으면 storeName을 사용하고 둘 다 없을 때만 `null`

### FD-11 — cursor 목록의 전체 개수

- [ ] **A. cursor 목록은 `pageInfo`만 제공한다. (권장)**
  - `/api/my/activity`는 `nextCursor`, `hasNext`만 제공한다.
  - 전체 count 쿼리를 피하고 변경 중인 타임라인의 부정확한 총계를 노출하지 않는다.
- [ ] **B. cursor 목록에도 `totalCount`를 제공한다.**
  - 응답 시점 전체 개수이며 다음 페이지를 조회하는 동안 값이 바뀔 수 있다.

두 선택 모두 cursor는 사용자·필터·첫 페이지 snapshot에 결속하며, 동일 시각 데이터도 중복·누락되지 않는 안정 정렬을 서버가 구현한다.

### FD-12 — 과거 날짜-only 활동의 시간 표현

- [ ] **A. 정확한 시각과 날짜-only 값을 분리한다. (권장)**
  - 정확한 시각이 있으면 `occurredAt`에 UTC 시각, `occurredOn`에 `null`, `timePrecision`에 `EXACT`를 반환한다.
  - 날짜만 있으면 `occurredAt`에 `null`, `occurredOn`에 `YYYY-MM-DD`, `timePrecision`에 `DATE`를 반환한다.
  - 앱은 `DATE` 항목에 임의의 시·분을 붙이지 않는다.

```json
{
  "occurredAt": null,
  "occurredOn": "2026-07-15",
  "timePrecision": "DATE"
}
```
- [ ] **B. 정확한 시각이 없는 기존 활동을 타임라인에서 제외한다.**
- [ ] **C. 신규 timestamp 마이그레이션이 완료될 때까지 타임라인 출시를 보류한다.**

날짜-only 값을 임의의 정확한 방문 시각으로 표시하는 선택지는 제공하지 않는다.

### FD-13 — 신규 허브 실패 시 기존 API fallback

- [ ] **A. 상태별 제한 fallback을 사용한다. (권장)**
  - `404`, `501` 또는 명시적인 feature-disabled 응답: 기존 API fallback
  - `401`: 토큰 갱신 후 신규 API 한 번 재호출, 실패하면 로그인 처리
  - `400`, `403`: fallback하지 않고 오류 처리
  - `5xx`: 제한된 횟수만 fallback하며 신규·기존 API를 무한 동시 호출하지 않음
- [ ] **B. fallback 없이 신규 허브 성공 후에만 앱을 전환한다.**
  - 출시 전 백엔드와 앱의 동시 배포 및 강한 사전 검증이 필요하다.

## 5. 백엔드가 결정하고 수행할 항목

아래 항목은 모바일 회신을 바탕으로 백엔드가 설계·구현한다. 모바일이 내부 구현 방법을 선택할 필요는 없다.

1. 친구 요청의 발신자·수신자·상태 전이 및 수신 대기 count 수정
2. 기존 `visitedStoresCount` 오계산 제거
3. 이미지 공개 상태 저장과 기존 데이터 기본값 마이그레이션
4. 본인·친구·비친구·양방향 차단·탈퇴를 포함한 공통 visibility 정책
5. 방문 그룹 ID, canonical 장소 ID 및 실제 방문 시각 스키마
6. UTC 저장·변환과 과거 날짜 데이터 처리
7. source-prefixed `activityId`와 서명된 keyset cursor
8. 허브 전용 집계 쿼리, 인덱스, query-count 및 성능 검증
9. 비공개 콘텐츠 URL 전달 방식
10. PostgreSQL 통합·권한·cursor 연속성·계약 테스트

## 6. 권장안 적용 시 Phase 1 응답 범위

FD-01~FD-13에서 권장안을 선택하면 Phase 1 허브는 방문·지도 필드를 제외한 다음 형태가 된다. 아래 예시는 미리보기 배열을 요청하지 않는 `GET /api/my/hub?previewLimit=0` 응답이다.

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
  "requestId": "server-request-id",
  "timestamp": "2026-07-16T09:00:00Z"
}
```

Phase 1 권장안에서는 아래 필드를 반환하지 않는다.

- `visitedPlaceCount`
- `recentVisits`
- `VISITED_PLACES`
- `FOOD_MAP`
- `FRIEND_ACTIVITY`
- `TASTE_PROFILE`

이 필드들은 해당 기능의 endpoint와 화면이 준비된 뒤 additive change로 추가한다.

## 7. 모바일 팀 회신 양식

아래 블록을 복사해 각 선택 결과를 회신한다.

```text
[Plate 마이페이지 API 계약 결정 회신]

FD-01 Phase 1 방문·지도 범위: A / B
FD-02 응답 envelope: A / B
FD-03 좋아요 카운트 의미: A / B / C
FD-04 콘텐츠 카운트 범위: A / B / C
FD-05 방문 의미: A / B / C
FD-06 친구 방문 활동: A / B / C
FD-07 지도 장소 기준: A / B / C
FD-07 기본 지도 type: ALL / POSTED / LIKED / VISITED
FD-08 primaryAction: A / B / C
FD-08 우선순위 변경: 없음 / (원하는 순서)
FD-09 availableSections: A / B
FD-10 콘텐츠 ID·placeId: A / B / C
FD-11 cursor 목록 totalCount: A / B
FD-12 날짜-only 활동: A / B / C
FD-13 fallback: A / B

지원할 최소 앱 버전:
신규 enum 무시 처리 가능 여부: 가능 / 불가
추가 JSON 필드 또는 화면 요구사항:
결정 참여자:
결정일:
```

모든 권장안을 그대로 수용하는 경우에는 다음과 같이 간단히 회신할 수 있다.

```text
Plate 마이페이지 API 계약 결정 요청서의 권장안 전체를 승인합니다.
방문·지도·친구 방문 활동은 Phase 2에서 별도 결정합니다.
지원할 최소 앱 버전:
담당자:
결정일:
```

## 8. 회신 후 진행

1. 백엔드는 선택 결과를 반영한 `Accepted` API 계약과 정확한 JSON 예시를 작성한다.
2. 모바일 팀은 JSON 필드·enum·null·fallback 계약을 확인한다.
3. 백엔드는 결정이 필요 없는 기존 오류와 보안 정책을 먼저 수정한다.
4. 스키마 변경이 필요하면 운영 DB 적용 계획을 별도로 승인받는다.
5. 백엔드 통합 테스트와 모바일 contract test가 모두 통과한 뒤 신규 허브를 활성화한다.

본 문서에 대한 회신 전에는 방문·타임라인·지도 계약을 확정된 것으로 간주하지 않는다.
