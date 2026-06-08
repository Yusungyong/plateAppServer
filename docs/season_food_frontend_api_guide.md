# 제철음식 프론트 API 요청/응답 가이드

문서 버전: 0.1  
대상: Android, iOS, Web 프론트  
관련 문서:

- `docs/season_food_api_design.md`
- `docs/season_food_store_matching_design.md`
- `docs/sql/season_food_schema_fp_330_339.sql`
- `docs/sql/season_food_store_matching_schema_fp_340_343.sql`

목적: 프론트에서 제철음식 목록, 상세, 카테고리, 지역, 제철음식 파는 집 화면을 구현할 때 필요한 API 호출 방식과 응답 필드를 정리한다.

---

## 구현 상태

기준일: 2026-06-08

프론트에서 호출 가능한 공개 API는 아래 8개다.

| 상태 | API | 용도 |
|---|---|---|
| 구현 완료 | `GET /api/season-foods` | 제철음식 목록 |
| 구현 완료 | `GET /api/season-foods/{ingredientId}` | 제철음식 상세 |
| 구현 완료 | `GET /api/season-food-categories` | 카테고리 트리 |
| 구현 완료 | `GET /api/season-regions` | 지역 트리 |
| 구현 완료 | `GET /api/season-foods/{ingredientId}/stores` | 특정 제철음식을 파는 집 목록 |
| 구현 완료 | `GET /api/season-stores/nearby` | 내 주변 제철음식 파는 집 |
| 구현 완료 | `GET /api/stores/{storeId}/season-foods` | 집 상세의 제철 메뉴 배지 |
| 구현 완료 | `GET /api/season-stores/home` | 홈용 제철 집 섹션 |

관리자 화면용 API도 구현되어 있다.

| 상태 | API | 용도 |
|---|---|---|
| 구현 완료 | `GET /api/admin/season-match-keywords` | 매칭 키워드 목록 |
| 구현 완료 | `POST /api/admin/season-match-keywords` | 매칭 키워드 등록 |
| 구현 완료 | `PUT /api/admin/season-match-keywords/{keywordId}` | 매칭 키워드 수정 |
| 구현 완료 | `DELETE /api/admin/season-match-keywords/{keywordId}` | 매칭 키워드 비활성화 |
| 구현 완료 | `GET /api/admin/season-store-matches` | 매칭 후보 목록 |
| 구현 완료 | `GET /api/admin/season-store-matches/{matchId}` | 매칭 후보 상세/근거 |
| 구현 완료 | `POST /api/admin/season-store-matches/{matchId}/confirm` | 매칭 확정 |
| 구현 완료 | `POST /api/admin/season-store-matches/{matchId}/reject` | 매칭 제외 |
| 미구현 | `POST /api/admin/season-store-matches/rebuild` | 자동 매칭 배치 재생성 |

주의사항:

1. `fp_330` ~ `fp_339` 테이블이 있어야 제철음식 정보 API가 정상 동작한다.
2. `fp_340` ~ `fp_343` 테이블과 `fp_341` 매칭 캐시 데이터가 있어야 집 추천/매칭 API가 의미 있는 결과를 반환한다.
3. `fp_340` ~ `fp_343` 테이블이 없으면 매칭 API는 `409 SEASON_STORE_MATCH_NOT_READY`를 반환한다.
4. 공개 조회 API는 인증 없이 호출 가능하다. 관리자 API는 기존 `/api/admin/**` 권한 정책을 따른다.

---

## 1. 공통 규칙

### 1.1 Base URL

환경별 base URL은 기존 앱 설정을 따른다.

```text
{BASE_URL}/api/...
```

### 1.2 Header

공개 조회 API는 인증 없이 호출 가능하도록 설계한다.

```http
Accept: application/json
Content-Type: application/json
```

위치 기반 개인화나 사용자별 제외 로직이 붙는 경우 기존 access token을 함께 보낼 수 있다.

```http
Authorization: Bearer {accessToken}
```

### 1.3 성공 응답 공통 형태

```json
{
  "success": true,
  "data": {},
  "requestId": "optional-request-id",
  "timestamp": "2026-06-08T09:00:00Z"
}
```

프론트는 기본적으로 `success === true`이면 `data`를 사용한다.

### 1.4 실패 응답 공통 형태

```json
{
  "success": false,
  "errorCode": "SEASON_FOOD_INVALID_MONTH",
  "message": "month는 1부터 12 사이여야 합니다.",
  "requestId": "optional-request-id",
  "timestamp": "2026-06-08T09:00:00Z"
}
```

프론트는 `message`를 우선 표시하고, 필요하면 `errorCode`별 fallback 문구를 사용한다.

### 1.5 월 기준

`month`는 1부터 12까지의 숫자다.

요청에서 `month`를 생략하면 서버가 현재 월을 사용한다. 앱에서 월 탭이나 캘린더를 운영한다면 명시적으로 보내는 것을 권장한다.

```text
1 = 1월
12 = 12월
```

---

## 2. 제철음식 목록

### 2.1 요청

```http
GET /api/season-foods?month=12&categoryId=CAT_SEAFOOD&regionId=REG_ALL&page=0&size=20
```

### 2.2 Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 1~12 |
| `categoryId` | string | N | null | 식재료 카테고리 |
| `regionId` | string | N | `REG_ALL` | 지역 필터 |
| `minScore` | number | N | 60 | 최소 제철 점수 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 목록 개수 |

### 2.3 응답 예시

```json
{
  "success": true,
  "data": {
    "month": 12,
    "regionId": "REG_ALL",
    "items": [
      {
        "ingredientId": "ING_GUL",
        "name": "굴",
        "aliases": ["석화", "생굴"],
        "category": {
          "categoryId": "CAT_SHELLFISH",
          "name": "패류"
        },
        "representativeRegion": {
          "regionId": "REG_SOUTH_SEA",
          "name": "남해"
        },
        "thumbnailUrl": null,
        "season": {
          "seasonId": "SEA_GUL_ALL",
          "displayText": "11~3월 제철 · 12~2월 절정",
          "seasonType": "TASTE",
          "confidence": "MEDIUM",
          "startMonth": 11,
          "endMonth": 3,
          "peakMonthText": "12~2월"
        },
        "monthScore": {
          "month": 12,
          "seasonScore": 95,
          "isPeak": true,
          "scoreLabel": "PEAK"
        },
        "summary": "겨울철 대표 패류로 국밥, 찜, 전 등으로 활용된다."
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  },
  "timestamp": "2026-06-08T09:00:00Z"
}
```

### 2.4 프론트 표시 가이드

| 필드 | 화면 활용 |
|---|---|
| `name` | 카드 타이틀 |
| `thumbnailUrl` | 카드 이미지. 없으면 기본 이미지 |
| `season.displayText` | 제철 기간 문구 |
| `monthScore.seasonScore` | 정렬/점수 UI |
| `monthScore.isPeak` | 절정 배지 |
| `category.name` | 필터 또는 보조 라벨 |
| `summary` | 카드 설명 |

---

## 3. 제철음식 상세

### 3.1 요청

```http
GET /api/season-foods/ING_GUL?month=12
```

### 3.2 응답 예시

```json
{
  "success": true,
  "data": {
    "ingredientId": "ING_GUL",
    "name": "굴",
    "aliases": ["석화", "생굴"],
    "category": {
      "categoryId": "CAT_SHELLFISH",
      "name": "패류"
    },
    "representativeRegion": {
      "regionId": "REG_SOUTH_SEA",
      "name": "남해"
    },
    "description": "겨울철 대표 패류로 국밥, 찜, 전 등으로 활용된다.",
    "storageTip": "신선도가 중요하므로 구입 후 빠르게 조리한다.",
    "thumbnailUrl": null,
    "currentMonthScore": {
      "month": 12,
      "seasonScore": 95,
      "isPeak": true,
      "scoreLabel": "PEAK",
      "description": "겨울철 풍미가 좋은 절정 구간이다."
    },
    "seasons": [
      {
        "seasonId": "SEA_GUL_ALL",
        "region": {
          "regionId": "REG_ALL",
          "name": "전국"
        },
        "seasonType": "TASTE",
        "startMonth": 11,
        "endMonth": 3,
        "peakMonthText": "12~2월",
        "displayText": "11~3월 제철 · 12~2월 절정",
        "confidence": "MEDIUM",
        "description": "겨울철 살이 차고 굴 특유의 풍미가 좋아지는 시기로 안내한다.",
        "monthScores": [
          {
            "month": 11,
            "seasonScore": 70,
            "isPeak": false,
            "scoreLabel": "EARLY"
          },
          {
            "month": 12,
            "seasonScore": 95,
            "isPeak": true,
            "scoreLabel": "PEAK"
          }
        ],
        "reasons": [
          {
            "reasonCode": "TASTE_PEAK",
            "title": "겨울 풍미",
            "description": "겨울철 굴의 풍미와 식감을 기대하기 좋은 시기로 안내한다."
          }
        ],
        "sources": [
          {
            "sourceType": "INTERNAL",
            "name": "초기 서비스 제철 기준",
            "url": null,
            "checkedAt": "2026-06-08",
            "reliability": "MEDIUM"
          }
        ]
      }
    ],
    "dishes": [
      {
        "dishId": "DISH_GUL_GUKBAP",
        "name": "굴국밥",
        "cookingType": "SOUP",
        "thumbnailUrl": null,
        "isRepresentative": true,
        "recommendMonths": [12, 1, 2],
        "description": "겨울 굴을 따뜻한 국물로 즐기는 대표 방식이다."
      }
    ]
  }
}
```

### 3.3 프론트 표시 가이드

| 필드 | 화면 활용 |
|---|---|
| `description` | 상세 소개 |
| `storageTip` | 보관 팁 섹션 |
| `currentMonthScore` | 현재 월 제철 상태 |
| `seasons[].monthScores` | 월별 그래프 |
| `seasons[].reasons` | 왜 지금 좋은지 |
| `dishes` | 활용 요리 칩 또는 리스트 |
| `sources` | 출처 표시. MVP에서는 숨겨도 됨 |

---

## 4. 카테고리 목록

### 4.1 요청

```http
GET /api/season-food-categories
```

### 4.2 응답 예시

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "categoryId": "CAT_SEAFOOD",
        "parentCategoryId": null,
        "name": "수산물",
        "level": 1,
        "children": [
          {
            "categoryId": "CAT_SHELLFISH",
            "parentCategoryId": "CAT_SEAFOOD",
            "name": "패류",
            "level": 2,
            "children": []
          }
        ]
      }
    ]
  }
}
```

### 4.3 프론트 사용

목록 필터, 홈 탭, 탐색 화면에 사용한다.

`children`이 있으므로 트리 UI와 평면 필터 UI 모두 만들 수 있다.

---

## 5. 지역 목록

### 5.1 요청

```http
GET /api/season-regions
```

### 5.2 응답 예시

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "regionId": "REG_ALL",
        "parentRegionId": null,
        "name": "전국",
        "type": "COUNTRY",
        "children": [
          {
            "regionId": "REG_JEJU",
            "parentRegionId": "REG_ALL",
            "name": "제주",
            "type": "PROVINCE",
            "children": []
          }
        ]
      }
    ]
  }
}
```

### 5.3 프론트 사용

초기에는 `REG_ALL`만 사용해도 된다.

지역별 제철 차이를 화면에 노출할 때 `REG_JEJU`, `REG_SOUTH_SEA` 같은 값을 필터로 사용한다.

---

## 6. 특정 제철음식을 파는 집 목록

이 API는 매칭 테이블 `fp_340` ~ `fp_343` 추가 후 사용한다.

### 6.1 요청

```http
GET /api/season-foods/ING_GUL/stores?month=12&lat=37.5665&lng=126.9780&radiusM=3000&page=0&size=20
```

### 6.2 Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 현재 월 기준 |
| `lat` | number | N | null | 위도 |
| `lng` | number | N | null | 경도 |
| `radiusM` | number | N | null | 반경 |
| `regionId` | string | N | null | 지역 필터 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 목록 개수 |

### 6.3 응답 예시

```json
{
  "success": true,
  "data": {
    "ingredient": {
      "ingredientId": "ING_GUL",
      "name": "굴",
      "seasonScore": 95,
      "isPeak": true
    },
    "items": [
      {
        "matchId": 1001,
        "storeId": 123,
        "restaurantId": null,
        "placeId": "google-place-id",
        "storeName": "통영굴밥",
        "address": "서울 ...",
        "representativeMenuName": "굴국밥",
        "representativeImageUrl": "https://...",
        "matchedKeywords": ["굴국밥", "굴"],
        "reasonText": "지금 제철인 굴 메뉴가 있어요",
        "matchScore": 142.5,
        "seasonScore": 95,
        "distanceM": 820,
        "matchStatus": "AUTO",
        "matchSource": "FP320_MENU"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

### 6.4 프론트 표시 가이드

| 필드 | 화면 활용 |
|---|---|
| `storeName` | 집 이름 |
| `representativeMenuName` | 제철 메뉴명 |
| `representativeImageUrl` | 카드 이미지 |
| `reasonText` | 카드 보조 문구 |
| `distanceM` | 내 주변 정렬/거리 표시 |
| `matchedKeywords` | 디버그 또는 운영 확인용. 일반 화면에서는 숨겨도 됨 |
| `matchStatus` | `CONFIRMED`면 신뢰 배지 가능 |

---

## 7. 내 주변 제철음식 파는 집

이 API는 매칭 테이블 추가 후 사용한다.

### 7.1 요청

```http
GET /api/season-stores/nearby?month=12&lat=37.5665&lng=126.9780&radiusM=3000&limit=20
```

### 7.2 응답 예시

```json
{
  "success": true,
  "data": {
    "month": 12,
    "items": [
      {
        "ingredientId": "ING_GUL",
        "ingredientName": "굴",
        "storeId": 123,
        "placeId": "google-place-id",
        "storeName": "통영굴밥",
        "representativeMenuName": "굴국밥",
        "representativeImageUrl": "https://...",
        "seasonScore": 95,
        "isPeak": true,
        "distanceM": 820,
        "reasonText": "지금 제철인 굴 메뉴가 있어요"
      }
    ]
  }
}
```

### 7.3 프론트 표시 가이드

지도, 주변 탐색, 홈 근처 추천 섹션에 사용한다.

위치 권한이 없으면 이 API 대신 `GET /api/season-stores/home` 또는 `GET /api/season-foods`를 사용한다.

---

## 8. 집 상세의 제철 메뉴 배지

이 API는 매칭 테이블 추가 후 사용한다.

### 8.1 요청

```http
GET /api/stores/123/season-foods?month=12
```

### 8.2 응답 예시

```json
{
  "success": true,
  "data": {
    "storeId": 123,
    "items": [
      {
        "ingredientId": "ING_GUL",
        "ingredientName": "굴",
        "representativeMenuName": "굴전",
        "seasonScore": 95,
        "isPeak": true,
        "reasonText": "12월은 굴이 제철이에요"
      }
    ]
  }
}
```

### 8.3 프론트 표시 가이드

집 상세 상단 또는 메뉴 영역에 배지로 표시한다.

예:

```text
제철 메뉴 · 굴전
지금 굴이 제철이에요
```

---

## 9. 홈용 제철 집 섹션

이 API는 매칭 테이블 추가 후 사용한다.

### 9.1 요청

```http
GET /api/season-stores/home?month=12&lat=37.5665&lng=126.9780&limit=10
```

위치값은 선택이다.

### 9.2 응답 예시

```json
{
  "success": true,
  "data": {
    "sections": [
      {
        "sectionId": "SEASON_ING_GUL",
        "title": "지금 굴 먹기 좋은 집",
        "ingredientId": "ING_GUL",
        "items": [
          {
            "storeId": 123,
            "placeId": "google-place-id",
            "storeName": "통영굴밥",
            "representativeMenuName": "굴국밥",
            "thumbnailUrl": "https://...",
            "reasonText": "지금 제철인 굴 메뉴가 있어요"
          }
        ]
      }
    ]
  }
}
```

---

## 10. 관리자 매칭 운영 API

관리자 화면에서 제철음식-집 매칭 품질을 운영하기 위한 API다. 모든 요청은 기존 `/api/admin/**` 권한 정책을 따르므로 관리자 access token이 필요하다.

### 10.1 키워드 사전 목록

```http
GET /api/admin/season-match-keywords?ingredientId=ING_GUL&keywordType=ALIAS&keyword=생굴&page=0&size=20
```

Query Parameters:

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `ingredientId` | string | N | null | 식재료 필터 |
| `keywordType` | string | N | null | `BASE`, `ALIAS`, `DISH`, `MENU`, `EXCLUDE` |
| `keyword` | string | N | null | 키워드 검색어 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 최대 50 |

응답 `data`:

```json
{
  "items": [
    {
      "keywordId": 1,
      "ingredientId": "ING_GUL",
      "ingredientName": "굴",
      "keyword": "생굴",
      "keywordType": "ALIAS",
      "matchWeight": 1.2,
      "exactMatch": false,
      "description": "굴 별칭",
      "useYn": "Y",
      "createdAt": "2026-06-08T12:00:00Z",
      "updatedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

### 10.2 키워드 등록/수정/비활성화

```http
POST /api/admin/season-match-keywords
PUT /api/admin/season-match-keywords/{keywordId}
DELETE /api/admin/season-match-keywords/{keywordId}
```

등록/수정 요청 Body:

```json
{
  "ingredientId": "ING_GUL",
  "keyword": "생굴",
  "keywordType": "ALIAS",
  "matchWeight": 1.2,
  "exactMatch": false,
  "description": "굴 별칭"
}
```

`DELETE`는 실제 삭제가 아니라 `useYn = "N"` 비활성화로 처리한다.

### 10.3 매칭 후보 목록

```http
GET /api/admin/season-store-matches?ingredientId=ING_GUL&status=AUTO&source=FP320_MENU&keyword=굴국밥&page=0&size=20
```

Query Parameters:

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `ingredientId` | string | N | null | 식재료 필터 |
| `status` | string | N | null | `AUTO`, `CONFIRMED`, `REJECTED` |
| `source` | string | N | null | `FP320_MENU`, `RESTAURANT_MENU`, `FEED`, `VIDEO`, `MIXED`, `MANUAL` |
| `keyword` | string | N | null | 집 이름, 메뉴명, 매칭 키워드 검색 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 최대 50 |

응답 `data.items[]`:

```json
{
  "matchId": 1001,
  "ingredientId": "ING_GUL",
  "ingredientName": "굴",
  "storeId": 123,
  "restaurantId": null,
  "placeId": "google-place-id",
  "storeName": "통영굴마을",
  "representativeMenuName": "굴국밥",
  "matchScore": 142.5,
  "seasonScore": 95,
  "evidenceCount": 2,
  "matchStatus": "AUTO",
  "matchSource": "FP320_MENU",
  "matchedKeywords": "굴국밥,굴",
  "generatedAt": "2026-06-08T12:00:00Z",
  "expiresAt": null
}
```

### 10.4 매칭 후보 상세/근거

```http
GET /api/admin/season-store-matches/{matchId}
```

응답 `data`:

```json
{
  "match": {
    "matchId": 1001,
    "ingredientId": "ING_GUL",
    "ingredientName": "굴",
    "storeId": 123,
    "storeName": "통영굴마을",
    "representativeMenuName": "굴국밥",
    "matchStatus": "AUTO",
    "matchSource": "FP320_MENU"
  },
  "evidence": [
    {
      "evidenceId": 2001,
      "targetType": "FP320_MENU",
      "targetId": "MENU_123",
      "matchedField": "item_name",
      "matchedKeyword": "굴국밥",
      "matchedText": "통영 굴국밥",
      "fieldWeight": 1.0,
      "keywordWeight": 1.5,
      "score": 150.0,
      "createdAt": "2026-06-08T12:00:00Z"
    }
  ]
}
```

### 10.5 매칭 확정/제외

```http
POST /api/admin/season-store-matches/{matchId}/confirm
POST /api/admin/season-store-matches/{matchId}/reject
```

확정 요청 Body:

```json
{
  "representativeMenuName": "굴국밥",
  "representativeImageUrl": "https://...",
  "note": "운영자 확인 완료"
}
```

제외 요청 Body:

```json
{
  "note": "굴비 메뉴라 굴 매칭에서 제외"
}
```

성공 시 응답은 `GET /api/admin/season-store-matches/{matchId}`와 같은 상세 형태다.

---

## 11. TypeScript 타입 예시

```ts
export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  requestId?: string;
  timestamp?: string;
};

export type SeasonFoodListResponse = {
  month: number;
  regionId: string;
  items: SeasonFoodListItem[];
  page: number;
  size: number;
  hasNext: boolean;
};

export type SeasonFoodListItem = {
  ingredientId: string;
  name: string;
  aliases: string[];
  category: SeasonCategorySummary;
  representativeRegion?: SeasonRegionSummary | null;
  thumbnailUrl?: string | null;
  season: SeasonSummary;
  monthScore: SeasonMonthScore;
  summary?: string | null;
};

export type SeasonCategorySummary = {
  categoryId: string;
  name: string;
};

export type SeasonRegionSummary = {
  regionId: string;
  name: string;
};

export type SeasonSummary = {
  seasonId: string;
  displayText?: string | null;
  seasonType: string;
  confidence: string;
  startMonth: number;
  endMonth: number;
  peakMonthText?: string | null;
};

export type SeasonMonthScore = {
  month: number;
  seasonScore: number;
  isPeak: boolean;
  scoreLabel?: string | null;
  description?: string | null;
};

export type SeasonStoreItem = {
  matchId?: number;
  storeId?: number | null;
  restaurantId?: number | null;
  placeId?: string | null;
  storeName: string;
  address?: string | null;
  representativeMenuName?: string | null;
  representativeImageUrl?: string | null;
  matchedKeywords?: string[];
  reasonText?: string | null;
  matchScore?: number;
  seasonScore?: number;
  distanceM?: number | null;
  matchStatus?: string;
  matchSource?: string;
};

export type IngredientStoreResponse = {
  ingredient: {
    ingredientId: string;
    name: string;
    seasonScore?: number | null;
    isPeak: boolean;
  };
  items: SeasonStoreItem[];
  page: number;
  size: number;
  hasNext: boolean;
};

export type NearbySeasonStoresResponse = {
  month: number;
  items: Array<{
    ingredientId: string;
    ingredientName: string;
    storeId?: number | null;
    restaurantId?: number | null;
    placeId?: string | null;
    storeName: string;
    representativeMenuName?: string | null;
    representativeImageUrl?: string | null;
    seasonScore?: number | null;
    isPeak: boolean;
    distanceM?: number | null;
    reasonText?: string | null;
  }>;
};

export type StoreSeasonFoodsResponse = {
  storeId: number;
  items: Array<{
    ingredientId: string;
    ingredientName: string;
    representativeMenuName?: string | null;
    seasonScore?: number | null;
    isPeak: boolean;
    reasonText?: string | null;
  }>;
};

export type HomeSeasonStoresResponse = {
  sections: Array<{
    sectionId: string;
    title: string;
    ingredientId: string;
    items: Array<{
      matchId: number;
      storeId?: number | null;
      restaurantId?: number | null;
      placeId?: string | null;
      storeName: string;
      representativeMenuName?: string | null;
      thumbnailUrl?: string | null;
      distanceM?: number | null;
      reasonText?: string | null;
    }>;
  }>;
};

export type SeasonMatchKeyword = {
  keywordId: number;
  ingredientId: string;
  ingredientName: string;
  keyword: string;
  keywordType: "BASE" | "ALIAS" | "DISH" | "MENU" | "EXCLUDE";
  matchWeight: number;
  exactMatch: boolean;
  description?: string | null;
  useYn: "Y" | "N";
  createdAt: string;
  updatedAt?: string | null;
};

export type SeasonStoreMatchAdminItem = {
  matchId: number;
  ingredientId: string;
  ingredientName: string;
  storeId?: number | null;
  restaurantId?: number | null;
  placeId?: string | null;
  storeName?: string | null;
  representativeMenuName?: string | null;
  matchScore: number;
  seasonScore?: number | null;
  evidenceCount: number;
  matchStatus: "AUTO" | "CONFIRMED" | "REJECTED";
  matchSource: "FP320_MENU" | "RESTAURANT_MENU" | "FEED" | "VIDEO" | "MIXED" | "MANUAL";
  matchedKeywords?: string | null;
  generatedAt: string;
  expiresAt?: string | null;
};
```

---

## 12. 화면별 추천 호출 흐름

### 12.1 제철음식 홈

```text
1. GET /api/season-foods?month={currentMonth}
2. 위치 권한이 있으면 GET /api/season-stores/home?month={currentMonth}&lat={lat}&lng={lng}
3. 위치 권한이 없으면 제철음식 목록만 표시하거나 기본 홈 섹션 호출
```

### 12.2 제철음식 상세

```text
1. GET /api/season-foods/{ingredientId}?month={currentMonth}
2. 하단에 파는 집 섹션이 필요하면 GET /api/season-foods/{ingredientId}/stores
```

### 12.3 집 상세

```text
1. 기존 집 상세 API 호출
2. GET /api/stores/{storeId}/season-foods?month={currentMonth}
3. 응답 items가 있으면 제철 메뉴 배지 노출
```

### 12.4 지도/주변

```text
1. 위치 권한 확인
2. GET /api/season-stores/nearby?lat={lat}&lng={lng}&month={currentMonth}
3. 지도 마커 또는 리스트에 대표 제철 메뉴 노출
```

---

## 13. 빈 결과 처리

### 13.1 목록이 비어 있는 경우

```json
{
  "success": true,
  "data": {
    "month": 12,
    "regionId": "REG_ALL",
    "items": [],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

프론트는 오류로 처리하지 않는다.

### 13.2 매칭 API가 아직 준비되지 않은 경우

서버 코드는 구현되어 있지만 로컬/운영 DB에 `fp_340` ~ `fp_343` 테이블이 없거나 `fp_341` 매칭 캐시가 아직 준비되지 않은 경우다.

```json
{
  "success": false,
  "errorCode": "SEASON_STORE_MATCH_NOT_READY",
  "message": "제철음식 매칭 데이터가 아직 준비되지 않았습니다."
}
```

프론트는 해당 섹션만 숨긴다.

---

## 14. 에러 처리 가이드

| errorCode | 프론트 처리 |
|---|---|
| `SEASON_FOOD_INVALID_MONTH` | 요청 월 보정 후 재시도 또는 기본 월 사용 |
| `SEASON_FOOD_NOT_FOUND` | 상세 화면에서 찾을 수 없음 표시 |
| `SEASON_REGION_NOT_FOUND` | 지역 필터 초기화 |
| `SEASON_CATEGORY_NOT_FOUND` | 카테고리 필터 초기화 |
| `SEASON_STORE_MATCH_NOT_READY` | 집 추천 섹션 숨김 |
| `COMMON_INVALID_INPUT` | `message` 표시 |

---

## 15. MVP 우선순위

프론트 입장에서 1차 구현 우선순위는 다음과 같다.

```text
1. GET /api/season-foods
2. GET /api/season-foods/{ingredientId}
3. GET /api/season-food-categories
4. GET /api/season-foods/{ingredientId}/stores
5. GET /api/stores/{storeId}/season-foods
```

`/stores`, `/nearby`, `/home` 계열은 서버 구현이 완료되어 있다. 프론트에서는 `SEASON_STORE_MATCH_NOT_READY`가 오면 해당 섹션만 숨기고, `fp_341` 매칭 캐시가 채워진 환경에서는 정상 노출한다.

