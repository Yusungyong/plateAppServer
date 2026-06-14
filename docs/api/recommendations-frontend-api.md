# 추천 API 프론트 연동 명세

작성일: 2026-06-14

## GET `/api/recommendations`

홈 추천 surface별 섹션을 반환합니다. 인증 토큰이 있으면 인증 사용자 기준으로 동작하고, 토큰이 없으면 `guestId`/`isGuest=true` 기준으로 호출할 수 있습니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---:|---:|---|
| `surfaces` | string | 아니오 | 콤마 구분 surface. 기본값: `HOME_FEED,NEARBY,SEASONAL,FRIEND` |
| `limitPerSurface` | number | 아니오 | surface별 최대 개수. 기본 6, 최대 20 |
| `lat` | number | 아니오 | 사용자 현재 위도 |
| `lng` | number | 아니오 | 사용자 현재 경도 |
| `currentMonth` | number | 아니오 | 제철 추천 기준 월. 없으면 서버 현재 월 |
| `baseStoreId` | number | 아니오 | `STORE_DETAIL_SIMILAR` 기준 매장 ID |
| `baseFeedId` | number | 아니오 | `STORE_DETAIL_SIMILAR` 기준 이미지 피드 ID |
| `guestId` | string | 아니오 | 비로그인 사용자 식별자 |
| `isGuest` | boolean | 아니오 | 비로그인 호출 여부 |

### Supported Surfaces

| Surface | 상태 | 설명 |
|---|---|---|
| `HOME_FEED` | 구현 | 최신 영상/이미지 피드와 인기 신호 기반 추천 |
| `NEARBY` | 구현 | `lat`/`lng`가 있으면 3km 이내 거리 기반 추천, 없으면 인기 fallback |
| `SEASONAL` | 구현 | 월별 제철 메뉴 fallback 추천 |
| `FRIEND` | 구현 | 로그인 사용자는 친구 방문 기록 기반, guest는 인기 fallback |
| `STORE_DETAIL_SIMILAR` | 구현 | `baseStoreId` 또는 `baseFeedId`의 같은 장소 기반 추천 |

### Example

```http
GET /api/recommendations?surfaces=HOME_FEED,NEARBY,SEASONAL,FRIEND&limitPerSurface=6&lat=37.5665&lng=126.9780&currentMonth=6
Authorization: Bearer {accessToken}
```

```http
GET /api/recommendations?surfaces=HOME_FEED,SEASONAL&limitPerSurface=2&guestId=guest-abc&isGuest=true
```

### Response

```json
{
  "requestId": "reco-1c939fc6-3b78-407a-aba5-8be33cfe95da",
  "generatedAt": "2026-06-14T19:01:08.5849087",
  "sections": [
    {
      "key": "HOME_FEED",
      "title": "오늘의 추천",
      "subtitle": "최근 인기 콘텐츠와 위치 신호를 조합한 추천입니다.",
      "items": [
        {
          "id": "HOME_FEED:video:337",
          "surface": "HOME_FEED",
          "targetType": "VIDEO_FEED",
          "title": "토담골",
          "subtitle": "토담골",
          "storeId": 337,
          "placeId": "ChIJ...",
          "feedId": null,
          "videoFeedId": 337,
          "seasonalFoodId": null,
          "storeName": "토담골",
          "address": "경기도 고양시 일산동구 중산동 19-30 토담골",
          "category": null,
          "thumbnailUrl": "https://foodplayerbucket.s3.ap-northeast-2.amazonaws.com/...",
          "distanceM": null,
          "friendNames": [],
          "score": 10,
          "scoreBreakdown": {
            "nearby": 0,
            "categoryAffinity": 0,
            "friendSignal": 0,
            "popularity": 10,
            "seasonal": 0,
            "similarity": 0,
            "seenPenalty": 0
          },
          "reasonLabels": ["최근 인기"],
          "isSeen": false
        }
      ]
    }
  ]
}
```

### Target Types

| Target Type | 이동에 필요한 값 |
|---|---|
| `STORE` | `storeId`, 가능하면 `placeId` |
| `IMAGE_FEED` | `feedId` |
| `VIDEO_FEED` | `videoFeedId`, `storeId`, 가능하면 `placeId` |
| `SEASONAL_MENU` | `seasonalFoodId` |

### 구현 메모

- `GET /api/recommendations`는 비로그인 호출도 가능하도록 보안 공개 GET 경로에 포함되어 있습니다.
- `SEASONAL`은 현재 서버 워크스페이스에 제철음식 JPA/API 코드가 없어 월별 fallback 추천으로 구현되어 있습니다.
- `FRIEND`는 로그인 사용자에게 친구 방문 기록이 있을 때 친구 신호를 반영하고, guest 또는 친구 데이터가 없으면 인기 매장 fallback을 반환합니다.
