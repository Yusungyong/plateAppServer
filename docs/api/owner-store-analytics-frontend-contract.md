# Owner Store Analytics Frontend Contract

점주가 자신의 입점 식당 성과를 확인하기 위한 API 계약입니다.

## 공통

- Base path: `/api/owner/stores/{storeId}/analytics`
- Auth: 점주 JWT 필요
- `{storeId}`는 `restaurants.id`입니다.
- 점주 권한은 `store_owners.store_id = restaurants.id` 기준으로 확인합니다.
- `from`, `to`는 `YYYY-MM-DD` 형식이며 양 끝 날짜를 모두 포함합니다.
- 응답은 공통 `ApiResponse<T>` 래퍼로 내려갑니다.

```json
{
  "success": true,
  "data": {}
}
```

## 집계 연결 기준

점주 식당과 사용자 콘텐츠의 연결점은 `restaurant_id`입니다.

- 동영상 피드: `fp_300.restaurant_id = restaurants.id`
- 이미지 피드: `fp_400.restaurant_id = restaurants.id`
- `fp_300.store_id`는 동영상 콘텐츠 ID이며 `restaurants.id`가 아닙니다.
- `fp_400.feed_no`는 이미지 콘텐츠 ID이며 `restaurants.id`가 아닙니다.

`source`는 실제 집계에 사용된 콘텐츠 ID 목록을 내려줍니다.

```json
{
  "source": {
    "storeId": 5,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301, 298],
    "imageFeedIds": [1204, 1198],
    "matchStrategy": "restaurant_id",
    "hasLinkedVideoContent": true,
    "hasLinkedImageContent": true,
    "hasLinkedContent": true
  }
}
```

## 클라이언트 행동 이벤트 수집

서버가 자동으로 알 수 없는 식당 행동은 앱/프론트가 이벤트 API로 알려줘야 합니다.

```http
POST /api/restaurants/{restaurantId}/events
Content-Type: application/json
```

Auth는 optional입니다. 로그인 유저면 JWT를 같이 보내고, 비로그인/게스트면 `guestId`, `sessionId`, `deviceId`를 가능한 범위에서 보내면 됩니다.

### Request

```json
{
  "eventType": "DETAIL_VIEW",
  "eventUid": "client-generated-uuid",
  "isGuest": false,
  "guestId": null,
  "sessionId": "session-123",
  "deviceId": "device-abc",
  "surface": "store_detail",
  "source": "map",
  "menuId": null,
  "contentType": null,
  "contentId": null,
  "clientEventAt": "2026-07-13T12:34:56+09:00"
}
```

### Response

```json
{
  "success": true,
  "data": {
    "eventId": 91,
    "recorded": true,
    "eventType": "DETAIL_VIEW"
  }
}
```

`eventUid`는 중복 전송 방지용입니다. 같은 `eventUid`가 다시 오면 `recorded=false`로 내려갈 수 있습니다.

### Supported Event Types

| eventType | 프론트 호출 시점 | 점주 화면 의미 |
| --- | --- | --- |
| `DETAIL_VIEW` | 매장 상세 화면 진입 | 매장 상세 조회 |
| `MAP_IMPRESSION` | 지도 마커/매장 카드가 노출됨 | 지도 노출 |
| `SEARCH_IMPRESSION` | 검색 결과에 매장이 노출됨 | 검색 노출 |
| `PHONE_CLICK` | 전화 버튼 클릭 | 전화 관심 |
| `DIRECTION_CLICK` | 길찾기 버튼 클릭 | 방문 의도 |
| `SHARE_CLICK` | 공유 버튼 클릭 | 공유 관심 |
| `MENU_VIEW` | 메뉴 영역/메뉴 상세 조회 | 메뉴 조회 |
| `MENU_SAVE` | 메뉴 저장/찜 | 메뉴 저장 |
| `VISIT_CONVERSION` | 방문 인증/방문 기록 완료 | 방문 전환 |
| `REVIEW_CONVERSION` | 후기/피드 작성 완료 | 후기 전환 |

권장 호출 위치:

- 매장 상세 화면 mount 또는 최초 가시화: `DETAIL_VIEW`
- 지도 목록/마커가 화면에 실제 노출될 때: `MAP_IMPRESSION`
- 검색 결과 카드가 화면에 실제 노출될 때: `SEARCH_IMPRESSION`
- 전화/길찾기/공유 버튼 클릭 직후: `PHONE_CLICK`, `DIRECTION_CLICK`, `SHARE_CLICK`
- 메뉴 탭 진입 또는 메뉴 리스트 로드 완료: `MENU_VIEW`
- 방문 인증 또는 후기 작성 성공 후: `VISIT_CONVERSION`, `REVIEW_CONVERSION`

## 콘텐츠 등록 시 매핑

앱에서 동영상 또는 이미지 피드를 등록할 때 `restaurantId`를 넘기면 해당 입점 식당에 직접 연결됩니다.

동영상:

```http
POST /api/videos
Content-Type: multipart/form-data
```

이미지:

```http
POST /api/image-feeds
Content-Type: multipart/form-data
```

주요 필드:

- `restaurantId`: `restaurants.id`, optional
- `storeName`: 화면 표시 매장명
- `address`: 주소
- `placeId`: 지도 장소 ID, optional

`restaurantId`를 보내지 않으면 서버가 `storeName + address`가 정확히 하나의 `restaurants` row와 일치하는 경우에만 자동 연결합니다. 일치하지 않거나 여러 건이 일치하면 `restaurant_id = null`로 저장되어 점주 통계에 포함되지 않습니다.

이미지 피드 수정 API도 `restaurantId`를 받을 수 있습니다.

```http
PATCH /api/image-feeds/{feedId}
Content-Type: application/json
```

```json
{
  "content": "오늘의 방문 기록",
  "address": "서울시 ...",
  "storeName": "Plate Burger",
  "restaurantId": 5,
  "placeId": "place-123"
}
```

이미지 피드 등록/이미지 추가/정렬 응답에는 `restaurantId`가 포함됩니다.

## 1. Summary

```http
GET /api/owner/stores/{storeId}/analytics/summary?from=2026-07-01&to=2026-07-07
```

### 주요 의미

- `homeImpressions`: 비디오 + 이미지 홈 노출 합계
- `imageImpressions`: 이미지 홈 노출
- `videoViews`, `uniqueViewers`, `completedViews`, `averageWatchSeconds`, `completionRate`: 비디오 전용 지표
- `activeSaves`, `newSaves`: 비디오 저장 지표
- `activeImageLikes`, `newImageLikes`: 이미지 좋아요 지표
- `comments`: 비디오 댓글 + 이미지 댓글 합계
- `storeActions`: 식당 자체 행동 이벤트 요약
- `funnel`: `fp_370` 기반 비디오 이벤트 퍼널

```json
{
  "source": {
    "storeId": 5,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301],
    "imageFeedIds": [1204],
    "matchStrategy": "restaurant_id",
    "hasLinkedVideoContent": true,
    "hasLinkedImageContent": true,
    "hasLinkedContent": true
  },
  "from": "2026-07-01",
  "to": "2026-07-07",
  "metrics": [
    { "key": "homeImpressions", "label": "Home impressions", "value": 1200, "changeRate": 12.5, "unit": "count", "comparison": "previous_period" },
    { "key": "imageImpressions", "label": "Image impressions", "value": 300, "changeRate": 8.0, "unit": "count", "comparison": "previous_period" },
    { "key": "videoViews", "label": "Video views", "value": 340, "changeRate": -3.2, "unit": "count", "comparison": "previous_period" },
    { "key": "activeImageLikes", "label": "Active image likes", "value": 44, "changeRate": null, "unit": "count", "comparison": "current" },
    { "key": "storeDetailViews", "label": "Store detail views", "value": 210, "changeRate": 18.0, "unit": "count", "comparison": "previous_period" },
    { "key": "directionClicks", "label": "Direction clicks", "value": 16, "changeRate": 33.3, "unit": "count", "comparison": "previous_period" },
    { "key": "phoneClicks", "label": "Phone clicks", "value": 9, "changeRate": null, "unit": "count", "comparison": "previous_period" }
  ],
  "watch": {
    "totalViews": 340,
    "uniqueViewers": 210,
    "completedViews": 90,
    "averageWatchSeconds": 18.43,
    "completionRate": 0.2647
  },
  "engagement": {
    "impressions": 1200,
    "videoImpressions": 900,
    "imageImpressions": 300,
    "activeSaveCount": 82,
    "activeImageLikeCount": 44,
    "newSaveCount": 14,
    "newImageLikeCount": 7,
    "commentCount": 15
  },
  "storeActions": {
    "detailViews": 210,
    "mapImpressions": 80,
    "searchImpressions": 45,
    "phoneClicks": 9,
    "directionClicks": 16,
    "shareClicks": 3,
    "menuViews": 52,
    "menuSaves": 4,
    "visitConversions": 2,
    "reviewConversions": 1
  },
  "funnel": {
    "impressions": 900,
    "clicks": 180,
    "plays": 150,
    "completes": 70,
    "hides": 3,
    "reports": 1,
    "clickThroughRate": 0.2,
    "playRate": 0.1667,
    "completeRate": 0.4667
  }
}
```

## 2. Trends

```http
GET /api/owner/stores/{storeId}/analytics/trends?from=2026-07-01&to=2026-07-07&interval=day
```

- 현재 `interval=day`만 지원합니다.
- 최대 조회 기간은 93일입니다.
- `impressions`는 비디오 + 이미지 합계입니다.
- `saves`는 비디오 저장, `imageLikes`는 이미지 좋아요입니다.
- `comments`는 비디오 댓글 + 이미지 댓글 합계입니다.
- 식당 행동 이벤트도 일자별 필드로 같이 내려갑니다.

```json
{
  "from": "2026-07-01",
  "to": "2026-07-07",
  "interval": "day",
  "points": [
    {
      "date": "2026-07-01",
      "impressions": 130,
      "videoImpressions": 100,
      "imageImpressions": 30,
      "views": 42,
      "completedViews": 11,
      "saves": 2,
      "imageLikes": 5,
      "comments": 1,
      "detailViews": 18,
      "mapImpressions": 7,
      "searchImpressions": 4,
      "phoneClicks": 1,
      "directionClicks": 2,
      "shareClicks": 0,
      "menuViews": 5,
      "menuSaves": 1,
      "visitConversions": 0,
      "reviewConversions": 0
    }
  ]
}
```

## 3. Contents

```http
GET /api/owner/stores/{storeId}/analytics/contents?from=2026-07-01&to=2026-07-07&page=0&size=20
```

비디오와 이미지 콘텐츠가 같은 배열에 섞여 내려갑니다.

- `contentType`: `video` 또는 `image`
- `contentId`: 화면에서 공통으로 사용할 콘텐츠 ID
- `videoStoreId`: `contentType=video`일 때 `fp_300.store_id`
- `feedId`: `contentType=image`일 때 `fp_400.feed_no`
- 이미지 행은 시청 지표가 없으므로 `views`, `uniqueViewers`, `completedViews`, `averageWatchSeconds`, `completionRate`가 0입니다.
- 이미지 행의 `activeSaveCount`, `newSaveCount`는 이미지 좋아요 수로 내려갑니다.

```json
{
  "content": [
    {
      "contentType": "video",
      "contentId": 301,
      "videoStoreId": 301,
      "feedId": null,
      "title": "신메뉴 버거 소개",
      "storeName": "Plate Burger",
      "thumbnailUrl": "https://...",
      "createdAt": "2026-06-30",
      "impressions": 900,
      "views": 240,
      "uniqueViewers": 160,
      "completedViews": 62,
      "averageWatchSeconds": 20.1,
      "completionRate": 0.2583,
      "activeSaveCount": 61,
      "newSaveCount": 10,
      "commentCount": 6
    },
    {
      "contentType": "image",
      "contentId": 1204,
      "videoStoreId": null,
      "feedId": 1204,
      "title": "Plate Burger",
      "storeName": "Plate Burger",
      "thumbnailUrl": "https://...",
      "createdAt": "2026-07-02",
      "impressions": 300,
      "views": 0,
      "uniqueViewers": 0,
      "completedViews": 0,
      "averageWatchSeconds": 0,
      "completionRate": 0,
      "activeSaveCount": 44,
      "newSaveCount": 7,
      "commentCount": 9
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

정렬 우선순위:

1. `views` 내림차순
2. `impressions` 내림차순
3. `newSaveCount` 내림차순
4. `createdAt` 내림차순
5. `contentId` 내림차순

## Empty State

`source.hasLinkedContent=false`이면 점주 식당에 연결된 비디오/이미지 콘텐츠가 없는 상태입니다.

```json
{
  "source": {
    "videoStoreIds": [],
    "imageFeedIds": [],
    "hasLinkedVideoContent": false,
    "hasLinkedImageContent": false,
    "hasLinkedContent": false
  }
}
```

권장 문구:

- "아직 집계할 콘텐츠가 없습니다."
- "매장과 연결된 동영상 또는 이미지 피드가 생기면 노출, 반응, 댓글 데이터를 확인할 수 있습니다."

## Error Cases

| Case | HTTP | errorCode | Notes |
| --- | --- | --- | --- |
| 미로그인 | 401 | `AUTH_UNAUTHORIZED` | 점주 통계 조회는 JWT 필요 |
| 내 매장이 아님 | 404 | `COMMON_NOT_FOUND` | 소유권 노출 방지 |
| 이벤트 대상 식당 없음 | 404 | `COMMON_NOT_FOUND` | 이벤트 수집 API |
| 날짜 누락 | 400 | `COMMON_MISSING_PARAMETER` | `from`, `to` 필요 |
| 날짜 역전 | 400 | `COMMON_INVALID_INPUT` | `from <= to` |
| 미지원 eventType | 400 | `COMMON_INVALID_INPUT` | 이벤트 수집 API |
| trends 기간 초과 | 400 | `COMMON_INVALID_INPUT` | 최대 93일 |
| interval 미지원 | 400 | `COMMON_INVALID_INPUT` | 현재 `day`만 지원 |
