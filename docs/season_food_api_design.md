# 제철음식 API 설계문서

문서 버전: 0.1  
기준 SQL: `docs/sql/season_food_schema_fp_330_339.sql`  
관련 문서:

- `docs/season_food_server_share.md`
- `docs/season_food_store_matching_design.md`
- `docs/season_food_frontend_api_guide.md`

목적: `fp_330` ~ `fp_339` 제철음식 본체 스키마와 제철음식 판매 집 매칭 구조를 기준으로 서버 API 경계, 응답 규격, 구현 순서를 정의한다.

---

## 구현 상태

기준일: 2026-06-08

- [x] `GET /api/season-foods` 구현 완료
- [x] `GET /api/season-foods/{ingredientId}` 구현 완료
- [x] `GET /api/season-food-categories` 구현 완료
- [x] `GET /api/season-regions` 구현 완료
- [x] 위 4개 조회 API를 `com.plateapp.plate_main.seasonfood` 패키지에 생성
- [x] 위 4개 조회 API를 Spring Security 공개 GET 경로에 추가
- [ ] `GET /api/season-foods/{ingredientId}/stores` 미구현: `fp_340` ~ `fp_343` 매칭 스키마와 배치 결과 필요
- [ ] `GET /api/season-stores/nearby` 미구현: `fp_340` ~ `fp_343` 매칭 스키마와 위치 정렬 정책 필요
- [ ] `GET /api/stores/{storeId}/season-foods` 미구현: 매장-제철음식 매칭 결과 필요
- [ ] `GET /api/season-stores/home` 미구현: 홈 섹션 정책과 매칭 결과 필요
- [ ] 관리자 매칭 API 미구현: 키워드 사전, 매칭 후보, 확정/제외, 재빌드 배치 스키마 필요

운영 반영 전 선행 조건:

1. `docs/sql/season_food_schema_fp_330_339.sql`의 `DROP TABLE ... CASCADE` 제거 또는 개발용 SQL 분리
2. `fp_330` ~ `fp_339` 테이블을 운영 마이그레이션으로 적용
3. seed 코드 누락값(`REASON:TASTE_PEAK`, `COOKING_TYPE:PAN_FRIED`) 보강

---

## 1. API 계층 구분

제철음식 API는 두 계층으로 나눈다.

```text
제철음식 정보 API
  fp_330 ~ fp_339 기반
  식재료, 제철 기간, 월별 점수, 제철 이유, 활용 요리, 출처 제공

제철음식 판매 집 API
  fp_340 ~ fp_343 제안 구조 기반
  제철 키워드/메뉴 텍스트 매칭 결과를 집 단위로 제공
```

이 분리가 중요한 이유는 다음과 같다.

1. `fp_330` ~ `fp_339`는 제철음식 자체를 설명하는 기준 데이터다.
2. "굴을 파는 집", "방어회가 있는 집"은 기존 메뉴/피드/영상과의 매칭 결과다.
3. 매칭은 자동 점수, 관리자 보정, 캐시 만료 같은 운영 로직이 필요하므로 별도 계층으로 둔다.

---

## 2. 응답 공통 규격

신규 API는 `com.plateapp.plate_main.common.api.ApiResponse` 형태를 기준으로 한다.

### 2.1 성공

```json
{
  "success": true,
  "data": {},
  "requestId": "optional-request-id",
  "timestamp": "2026-06-08T09:00:00Z"
}
```

### 2.2 실패

```json
{
  "success": false,
  "errorCode": "SEASON_FOOD_INVALID_MONTH",
  "message": "month는 1부터 12 사이여야 합니다.",
  "requestId": "optional-request-id",
  "timestamp": "2026-06-08T09:00:00Z"
}
```

### 2.3 페이지 응답

신규 제철음식 API는 프론트 사용성을 위해 `page`, `size`, `hasNext`를 기본으로 한다.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

기존 공통 `PagedResponse`의 `limit`, `offset` 방식과 다르므로 구현 시 DTO를 별도로 두거나 컨트롤러에서 변환한다.

---

## 3. 공개 앱 API

### 3.1 제철음식 목록

```http
GET /api/season-foods
```

현재 월 또는 지정 월의 제철 식재료 목록을 제공한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 1~12 |
| `categoryId` | string | N | null | 예: `CAT_FRUIT`, `CAT_SEAFOOD` |
| `regionId` | string | N | `REG_ALL` | 예: `REG_ALL`, `REG_JEJU` |
| `minScore` | number | N | 60 | 최소 제철 점수 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 최대 50 권장 |

#### 주요 테이블

```text
fp_335_season_month_score
fp_334_season_window
fp_333_ingredient
fp_331_food_category
fp_332_region
```

#### 정렬

```text
is_peak_yn DESC
season_score DESC
ingredient.sort_order ASC
ingredient.ingredient_nm ASC
```

#### Response Data

```json
{
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
}
```

---

### 3.2 제철음식 상세

```http
GET /api/season-foods/{ingredientId}
```

식재료 상세, 제철 기간, 월별 점수, 이유, 활용 요리, 출처를 제공한다.

#### Path Parameters

| 이름 | 타입 | 설명 |
|---|---|---|
| `ingredientId` | string | 예: `ING_GUL` |

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `regionId` | string | N | null | 특정 지역 우선 조회 |
| `month` | number | N | 현재 월 | 현재 월 강조용 |

#### 주요 테이블

```text
fp_333_ingredient
fp_331_food_category
fp_332_region
fp_334_season_window
fp_335_season_month_score
fp_336_season_reason
fp_337_dish
fp_338_ingredient_dish
fp_339_season_source
```

#### Response Data

```json
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
```

---

### 3.3 제철음식 카테고리

```http
GET /api/season-food-categories
```

`fp_331_food_category` 기준 카테고리 트리를 제공한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `type` | string | N | `INGREDIENT` | `category_type_cd` |

#### Response Data

```json
{
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
```

---

### 3.4 제철음식 지역

```http
GET /api/season-regions
```

`fp_332_region` 기준 지역 트리를 제공한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `type` | string | N | null | 예: `COUNTRY`, `PROVINCE`, `SEA_AREA` |

#### Response Data

```json
{
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
```

---

## 4. 제철음식 판매 집 API

이 API는 `docs/season_food_store_matching_design.md`의 `fp_340` ~ `fp_343` 구조가 추가된 뒤 안정적으로 구현한다.

### 4.1 특정 제철음식을 파는 집 목록

```http
GET /api/season-foods/{ingredientId}/stores
```

예: 굴을 파는 집 목록.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 현재 월 기준 제철 점수 |
| `lat` | number | N | null | 위치 정렬 또는 반경 검색 |
| `lng` | number | N | null | 위치 정렬 또는 반경 검색 |
| `radiusM` | number | N | null | 반경 미터 |
| `regionId` | string | N | null | 제철 지역 또는 사용자 필터 |
| `page` | number | N | 0 | 0부터 시작 |
| `size` | number | N | 20 | 최대 50 권장 |

#### 주요 테이블

```text
fp_341_season_store_match
fp_342_season_match_evidence
fp_343_season_store_match_override
fp_333_ingredient
fp_334_season_window
fp_335_season_month_score
```

#### Response Data

```json
{
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
```

---

### 4.2 내 주변 제철음식 파는 집

```http
GET /api/season-stores/nearby
```

여러 제철 식재료를 섞어서 주변 집을 제공한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 현재 월 기준 |
| `lat` | number | Y | 없음 | 사용자 위도 |
| `lng` | number | Y | 없음 | 사용자 경도 |
| `radiusM` | number | N | 3000 | 반경 미터 |
| `categoryId` | string | N | null | 식재료 카테고리 |
| `limit` | number | N | 20 | 최대 50 권장 |

#### 정렬

```text
거리 가까움
season_score 높음
match_score 높음
CONFIRMED 우선
대표 이미지 있음 우선
```

#### Response Data

```json
{
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
```

---

### 4.3 집 상세의 제철 메뉴 배지

```http
GET /api/stores/{storeId}/season-foods
```

집 상세 화면에서 "이 집의 제철 메뉴" 배지를 표시한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 현재 월 기준 |

#### Response Data

```json
{
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
```

---

### 4.4 홈용 제철 집 섹션

```http
GET /api/season-stores/home
```

홈 화면에서 사용할 제철 집 섹션을 제공한다.

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `month` | number | N | 현재 월 | 현재 월 기준 |
| `lat` | number | N | null | 위치 기반 개인화 |
| `lng` | number | N | null | 위치 기반 개인화 |
| `limit` | number | N | 10 | 섹션별 최대 개수 |

#### Response Data

```json
{
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
```

---

## 5. 관리자 API

관리자 API는 MVP에서 전체 CRUD를 모두 열기보다 매칭 운영에 필요한 기능부터 제공한다.

### 5.1 키워드 사전 목록

```http
GET /api/admin/season-match-keywords
```

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `ingredientId` | string | N | 특정 식재료 필터 |
| `keywordType` | string | N | BASE, ALIAS, DISH, MENU, EXCLUDE |
| `keyword` | string | N | 검색어 |
| `page` | number | N | 기본 0 |
| `size` | number | N | 기본 20 |

### 5.2 키워드 등록

```http
POST /api/admin/season-match-keywords
```

```json
{
  "ingredientId": "ING_GUL",
  "keyword": "석화",
  "keywordType": "ALIAS",
  "matchWeight": 1.2,
  "exactMatch": false,
  "description": "굴의 별칭"
}
```

### 5.3 키워드 수정

```http
PUT /api/admin/season-match-keywords/{keywordId}
```

### 5.4 키워드 삭제 또는 비활성

```http
DELETE /api/admin/season-match-keywords/{keywordId}
```

실제 삭제보다 `use_yn = 'N'` 처리를 권장한다.

### 5.5 매칭 후보 목록

```http
GET /api/admin/season-store-matches
```

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `ingredientId` | string | N | 식재료 필터 |
| `status` | string | N | AUTO, CONFIRMED, REJECTED |
| `source` | string | N | FP320_MENU, RESTAURANT_MENU, FEED, VIDEO |
| `keyword` | string | N | 집 이름 또는 메뉴명 |
| `page` | number | N | 기본 0 |
| `size` | number | N | 기본 20 |

### 5.6 매칭 근거 상세

```http
GET /api/admin/season-store-matches/{matchId}
```

근거 원문, 점수, 매칭 필드, 매칭 키워드를 제공한다.

### 5.7 매칭 확정

```http
POST /api/admin/season-store-matches/{matchId}/confirm
```

```json
{
  "representativeMenuName": "굴국밥",
  "representativeImageUrl": "https://...",
  "note": "운영자 확인 완료"
}
```

### 5.8 매칭 제외

```http
POST /api/admin/season-store-matches/{matchId}/reject
```

```json
{
  "note": "굴비 메뉴라 굴 제철음식 매칭에서 제외"
}
```

### 5.9 매칭 배치 재실행

```http
POST /api/admin/season-store-matches/rebuild
```

```json
{
  "ingredientId": "ING_GUL",
  "month": 12,
  "dryRun": false
}
```

---

## 6. 에러 코드

| errorCode | HTTP | 설명 |
|---|---:|---|
| `SEASON_FOOD_INVALID_MONTH` | 400 | `month`가 1~12 범위를 벗어남 |
| `SEASON_FOOD_NOT_FOUND` | 404 | 식재료 없음 |
| `SEASON_REGION_NOT_FOUND` | 404 | 지역 없음 |
| `SEASON_CATEGORY_NOT_FOUND` | 404 | 카테고리 없음 |
| `SEASON_STORE_MATCH_NOT_READY` | 409 | 매칭 테이블 또는 배치 결과 없음 |
| `SEASON_STORE_MATCH_NOT_FOUND` | 404 | 매칭 결과 없음 |
| `SEASON_KEYWORD_DUPLICATED` | 409 | 키워드 중복 |
| `SEASON_KEYWORD_INVALID` | 400 | 키워드 값 오류 |
| `COMMON_INVALID_INPUT` | 400 | 공통 입력 오류 |

---

## 7. 구현 패키지 제안

```text
com.plateapp.plate_main.seasonfood
  controller
    SeasonFoodController
    SeasonStoreController
    AdminSeasonFoodMatchController
  service
    SeasonFoodQueryService
    SeasonFoodDetailService
    SeasonStoreMatchQueryService
    SeasonStoreMatchAdminService
    SeasonStoreMatchBatchService
  entity
    Fp330Code
    Fp331FoodCategory
    Fp332Region
    Fp333Ingredient
    Fp334SeasonWindow
    Fp335SeasonMonthScore
    Fp336SeasonReason
    Fp337Dish
    Fp338IngredientDish
    Fp339SeasonSource
    Fp340SeasonMatchKeyword
    Fp341SeasonStoreMatch
    Fp342SeasonMatchEvidence
    Fp343SeasonStoreMatchOverride
  repository
  dto
```

패키지명은 기존 삭제된 `seasonal`과 구분하기 위해 `seasonfood`를 권장한다.

---

## 8. SQL 선행 확인 사항

현재 `docs/sql/season_food_schema_fp_330_339.sql` 기준으로 선행 확인할 항목이 있다.

### 8.1 운영용 마이그레이션에서 DROP 제거

SQL 상단에 개발 재실행용 `DROP TABLE ... CASCADE`가 있다.

운영 반영 파일에서는 제거하거나 별도 개발용 파일로 분리한다.

### 8.2 seed 코드 누락 확인

현재 seed 데이터에서 사용하는 코드 중 `fp_330_code`에 없는 값이 있다.

```text
REASON: TASTE_PEAK
COOKING_TYPE: PAN_FRIED
```

둘 중 하나로 처리한다.

```text
1. fp_330_code seed에 코드 추가
2. seed 데이터에서 기존 코드로 치환
```

### 8.3 코드 FK 전략

`fp_330_code`는 공통 코드 테이블이지만 `season_type_cd`, `reason_cd`, `cooking_type_cd` 같은 컬럼이 DB FK로 강제되지는 않는다.

초기에는 애플리케이션 레벨 검증으로 충분하다. 운영 안정성이 필요하면 복합 FK 또는 체크 정책을 추가한다.

---

## 9. 구현 순서

### 1단계: 제철음식 조회 API

```text
GET /api/season-foods
GET /api/season-foods/{ingredientId}
GET /api/season-food-categories
GET /api/season-regions
```

이 단계는 `fp_330` ~ `fp_339`만으로 가능하다.

### 2단계: 집 매칭 스키마 추가

```text
fp_340_season_match_keyword
fp_341_season_store_match
fp_342_season_match_evidence
fp_343_season_store_match_override
```

### 3단계: 자동 매칭 배치

```text
제철 키워드 조회
fp_320 메뉴명/설명 검색
restaurant_menus 메뉴명/설명 검색
fp_400 피드 보조 검색
fp_300 영상 보조 검색
집 단위 병합 후 fp_341/fp_342 저장
```

### 4단계: 제철음식 파는 집 API

```text
GET /api/season-foods/{ingredientId}/stores
GET /api/season-stores/nearby
GET /api/stores/{storeId}/season-foods
GET /api/season-stores/home
```

### 5단계: 관리자 운영 API

```text
키워드 사전 관리
자동 매칭 후보 검수
확정/제외 처리
배치 재실행
```

