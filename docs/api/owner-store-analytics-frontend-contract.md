# Owner Store Analytics Frontend Contract

점주 관리자 화면에서 "내 매장 성과"를 보여주기 위한 API 계약입니다.

## 공통 사항

- Base path: `/api/owner/stores/{storeId}/analytics`
- Auth: 점주 로그인 JWT 필요
- 권한: `{storeId}`는 `store_owners`에 활성 소유자로 연결된 `restaurants.id`여야 합니다.
- 날짜: `from`, `to`는 `YYYY-MM-DD` 형식이며 양 끝 날짜를 모두 포함합니다.
- 응답 래퍼: 모든 API는 `ApiResponse<T>`로 감싸져 내려갑니다.

```json
{
  "success": true,
  "data": {}
}
```

## 집계 연결 기준

점주 권한 매장은 `restaurants.id` 기준입니다. 앱 콘텐츠/반응 데이터는 현재 `fp_300.store_id` 기준으로 쌓여 있어, 서버는 아래 규칙으로 점주 매장과 동영상 매장을 연결합니다.

1. 현재 매장의 활성 점주들이 업로드한 `fp_300` 콘텐츠만 집계합니다.
2. `fp_300.store_id == restaurants.id`이면 연결합니다.
3. 또는 `fp_300.store_name/title`과 `fp_300.address`가 `restaurants.title/address`와 일치하면 연결합니다.

각 응답의 `source.videoStoreIds`에 실제 집계에 사용된 `fp_300.store_id` 목록이 내려갑니다. 이 값이 비어 있으면 콘텐츠 기반 지표는 모두 `0`입니다.

```json
{
  "source": {
    "storeId": 12,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301, 298],
    "matchStrategy": "restaurant_id_or_owner_name_address",
    "hasLinkedVideoContent": true
  }
}
```

프론트 권장 처리:

- `hasLinkedVideoContent=false`: "아직 집계할 콘텐츠가 없습니다" 빈 상태 노출
- `videoStoreIds`는 디버그/운영 확인용으로 화면에 직접 노출하지 않아도 됩니다.

## 1. Summary

상단 카드, 핵심 요약, 퍼널 요약에 사용합니다.

```http
GET /api/owner/stores/{storeId}/analytics/summary?from=2026-07-01&to=2026-07-07
```

### Response data

```json
{
  "source": {
    "storeId": 12,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301],
    "matchStrategy": "restaurant_id_or_owner_name_address",
    "hasLinkedVideoContent": true
  },
  "from": "2026-07-01",
  "to": "2026-07-07",
  "metrics": [
    {
      "key": "homeImpressions",
      "label": "Home impressions",
      "value": 1200,
      "changeRate": 12.5,
      "unit": "count",
      "comparison": "previous_period"
    },
    {
      "key": "videoViews",
      "label": "Video views",
      "value": 340,
      "changeRate": -3.2,
      "unit": "count",
      "comparison": "previous_period"
    },
    {
      "key": "uniqueViewers",
      "label": "Unique viewers",
      "value": 210,
      "changeRate": 5.0,
      "unit": "count",
      "comparison": "previous_period"
    },
    {
      "key": "activeSaves",
      "label": "Active saves",
      "value": 82,
      "changeRate": null,
      "unit": "count",
      "comparison": "current"
    },
    {
      "key": "newSaves",
      "label": "New saves",
      "value": 14,
      "changeRate": 40.0,
      "unit": "count",
      "comparison": "previous_period"
    },
    {
      "key": "comments",
      "label": "Comments",
      "value": 9,
      "changeRate": 0.0,
      "unit": "count",
      "comparison": "previous_period"
    }
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
    "activeSaveCount": 82,
    "newSaveCount": 14,
    "commentCount": 9
  },
  "funnel": {
    "impressions": 1100,
    "clicks": 180,
    "plays": 150,
    "completes": 70,
    "hides": 3,
    "reports": 1,
    "clickThroughRate": 0.1636,
    "playRate": 0.1364,
    "completeRate": 0.4667
  }
}
```

### Field notes

- `metrics[].changeRate`: 직전 동일 기간 대비 증감률(%)입니다. 직전 기간 값이 0이고 현재 값이 있으면 `null`입니다.
- `watch.completionRate`: `completedViews / totalViews`
- `funnel.clickThroughRate`: `clicks / funnel.impressions`
- `funnel.playRate`: `plays / funnel.impressions`
- `funnel.completeRate`: `completes / plays`
- 비율 필드는 `0.2647`처럼 0~1 범위입니다. 화면에서는 `26.47%`처럼 포맷하면 됩니다.

### Suggested UI

- KPI 카드: `homeImpressions`, `videoViews`, `uniqueViewers`, `activeSaves`, `newSaves`, `comments`
- 시청 품질 카드: 평균 시청 시간, 완주율
- 추천 퍼널: 노출 -> 클릭 -> 재생 -> 완주

## 2. Trends

일자별 선그래프/막대그래프에 사용합니다.

```http
GET /api/owner/stores/{storeId}/analytics/trends?from=2026-07-01&to=2026-07-07&interval=day
```

### Constraints

- 현재 `interval=day`만 지원합니다.
- 최대 조회 기간은 93일입니다.

### Response data

```json
{
  "source": {
    "storeId": 12,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301],
    "matchStrategy": "restaurant_id_or_owner_name_address",
    "hasLinkedVideoContent": true
  },
  "from": "2026-07-01",
  "to": "2026-07-07",
  "interval": "day",
  "points": [
    {
      "date": "2026-07-01",
      "impressions": 130,
      "views": 42,
      "completedViews": 11,
      "saves": 2,
      "comments": 1
    },
    {
      "date": "2026-07-02",
      "impressions": 180,
      "views": 51,
      "completedViews": 15,
      "saves": 4,
      "comments": 0
    }
  ]
}
```

### Suggested UI

- 기본 그래프: `impressions`, `views`
- 보조 그래프: `saves`, `comments`
- 완주 성과: `completedViews`를 `views`와 함께 표시

## 3. Contents

콘텐츠별 성과 테이블/카드 목록에 사용합니다.

```http
GET /api/owner/stores/{storeId}/analytics/contents?from=2026-07-01&to=2026-07-07&page=0&size=20
```

### Response data

```json
{
  "source": {
    "storeId": 12,
    "storeName": "Plate Burger",
    "address": "서울시 ...",
    "videoStoreIds": [301, 298],
    "matchStrategy": "restaurant_id_or_owner_name_address",
    "hasLinkedVideoContent": true
  },
  "from": "2026-07-01",
  "to": "2026-07-07",
  "content": [
    {
      "videoStoreId": 301,
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
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

### Sorting

서버는 아래 우선순위로 정렬합니다.

1. 조회 수(`views`) 내림차순
2. 노출 수(`impressions`) 내림차순
3. 콘텐츠 생성일 내림차순
4. `videoStoreId` 내림차순

### Suggested UI

- 테이블 컬럼: 썸네일/제목, 노출, 조회, 완주율, 평균 시청 시간, 저장, 댓글
- 카드형 모바일 UI: 썸네일 + 핵심 3개 지표(`views`, `completionRate`, `newSaveCount`)

## Error cases

| Case | HTTP | errorCode | Notes |
| --- | --- | --- | --- |
| 미로그인 | 401 | `AUTH_UNAUTHORIZED` | JWT 필요 |
| 내 매장이 아님 | 404 | `COMMON_NOT_FOUND` | 소유권 노출 방지 목적 |
| 날짜 누락 | 400 | `COMMON_MISSING_PARAMETER` | `from`, `to` 필요 |
| 날짜 역전 | 400 | `COMMON_INVALID_INPUT` | `from <= to` |
| trends 기간 초과 | 400 | `COMMON_INVALID_INPUT` | 최대 93일 |
| interval 미지원 | 400 | `COMMON_INVALID_INPUT` | 현재 `day`만 지원 |

## Empty state guidance

아래 응답이면 화면에는 오류가 아니라 빈 상태를 보여주세요.

```json
{
  "source": {
    "hasLinkedVideoContent": false,
    "videoStoreIds": []
  },
  "metrics": [
    { "key": "homeImpressions", "value": 0 },
    { "key": "videoViews", "value": 0 }
  ]
}
```

권장 문구:

- "아직 집계할 콘텐츠가 없습니다."
- "매장 소개 영상이나 피드를 등록하면 노출, 시청, 저장 데이터를 확인할 수 있습니다."

