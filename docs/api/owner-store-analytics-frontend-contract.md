# Owner Store Analytics Frontend Contract

점주가 자신의 입점 식당 성과를 확인하기 위한 API 계약입니다.

## 공통

- Base path: `/api/owner/stores/{storeId}/analytics`
- Auth: 점주 JWT 필요
- `{storeId}`는 `restaurants.id`입니다.
- 권한은 `store_owners.store_id = restaurants.id` 기준으로 확인합니다.
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
    { "key": "activeSaves", "label": "Active saves", "value": 82, "changeRate": null, "unit": "count", "comparison": "current" },
    { "key": "activeImageLikes", "label": "Active image likes", "value": 44, "changeRate": null, "unit": "count", "comparison": "current" },
    { "key": "comments", "label": "Comments", "value": 15, "changeRate": 0.0, "unit": "count", "comparison": "previous_period" }
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
      "comments": 1
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
| 미로그인 | 401 | `AUTH_UNAUTHORIZED` | JWT 필요 |
| 내 매장이 아님 | 404 | `COMMON_NOT_FOUND` | 소유권 노출 방지 |
| 날짜 누락 | 400 | `COMMON_MISSING_PARAMETER` | `from`, `to` 필요 |
| 날짜 역전 | 400 | `COMMON_INVALID_INPUT` | `from <= to` |
| trends 기간 초과 | 400 | `COMMON_INVALID_INPUT` | 최대 93일 |
| interval 미지원 | 400 | `COMMON_INVALID_INPUT` | 현재 `day`만 지원 |
