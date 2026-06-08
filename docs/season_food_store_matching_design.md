# 제철음식 집 매칭 설계문서

문서 버전: 0.1  
기준 문서: `docs/season_food_server_share.md`  
목적: 제철음식 데이터를 앱의 핵심 가치인 "맛있는 집 찾기"와 연결하기 위해, 제철 식재료/요리 키워드를 기존 메뉴, 식당, 이미지, 동영상 콘텐츠와 텍스트 기반으로 매칭하는 서버 구조를 정의한다.

---

## 1. 배경

기존 제철음식 문서는 `fp_333_ingredient`를 중심으로 식재료, 제철 기간, 월별 점수, 제철 이유, 활용 요리를 관리한다.

하지만 우리 앱의 사용자 경험은 식재료 자체보다 다음 질문에 가깝다.

```text
지금 제철인 굴을 먹을 수 있는 집은 어디인가?
내 주변에 굴국밥, 굴전, 석화 파는 집이 있는가?
이 집에는 어떤 제철 메뉴와 이미지/영상이 있는가?
```

따라서 제철음식 데이터와 기존 앱 콘텐츠 사이에 다음 중간 계층이 필요하다.

```text
제철 식재료
→ 키워드/별칭/요리명 사전
→ 메뉴/식당/피드/영상 텍스트 매칭
→ 제철음식 판매 집 후보
→ 홈, 지도, 검색, 상세 화면 노출
```

---

## 2. 설계 원칙

1. 사용자에게는 식재료가 아니라 "집" 중심으로 노출한다.
2. 기존 콘텐츠 테이블에 제철음식 FK를 직접 심지 않는다.
3. 텍스트 기반 자동 매칭을 기본으로 하되, 관리자 보정이 가능해야 한다.
4. 메뉴명 직접 매칭을 가장 강한 근거로 본다.
5. 식당 카테고리는 보조 신호로만 사용하고, 제철음식 자체를 카테고리로 남발하지 않는다.
6. 홈/지도/추천 API에서 빠르게 조회할 수 있도록 매칭 결과를 캐시 테이블로 관리한다.
7. `굴비`, `동굴`, `굴착`처럼 오탐 가능성이 있는 단어는 제외어로 관리한다.

---

## 3. 기존 테이블 활용

### 3.1 앱 메뉴 테이블: `fp_320`

가장 중요한 매칭 대상이다.

| 컬럼 | 활용 |
|---|---|
| `item_id` | 메뉴 단위 식별자 |
| `store_id` | 앱 콘텐츠 기준 집 식별자 |
| `item_name` | 제철 키워드 직접 매칭 대상 |
| `menu_title` | 제철 키워드 직접 매칭 대상 |
| `description` | 제철 키워드 보조 매칭 대상 |
| `menu_image` | 메뉴 이미지 노출 |
| `feed_id` | 이미지 피드와 연결 가능한 보조 키 |
| `place_id` | 장소/지도/피드 그룹 연결 키 |
| `store_name` | 집 이름 표시 및 보조 연결 키 |

예시:

```text
ING_GUL
키워드: 굴, 생굴, 석화, 굴국밥, 굴전

fp_320.item_name = 굴국밥
→ 굴 제철음식 매칭
→ store_id/place_id/store_name 기준으로 집 후보 생성
```

### 3.2 관리자 식당 테이블: `restaurants`

관리자가 큐레이션한 식당 기본 정보다.

| 컬럼 | 활용 |
|---|---|
| `id` | 관리자 식당 ID |
| `title` | 식당명 |
| `address` | 위치 표시 및 지역 보조 매칭 |
| `introduction` | 식당 설명 텍스트 보조 매칭 |
| `exposure_status` | 노출 가능 여부 |

### 3.3 관리자 식당 메뉴 테이블: `restaurant_menus`

관리자가 입력한 메뉴 단위 매칭 대상이다.

| 컬럼 | 활용 |
|---|---|
| `id` | 관리자 메뉴 ID |
| `restaurant_id` | 식당 연결 |
| `name` | 제철 키워드 직접 매칭 대상 |
| `description` | 제철 키워드 보조 매칭 대상 |
| `price` | 메뉴 가격 표시 |

### 3.4 관리자 식당 카테고리: `restaurant_categories`

제철음식 직접 매칭용보다는 큰 분류 보조 신호로 사용한다.

예:

```text
굴 → 해산물/한식 카테고리 식당 가중치
방어 → 해산물/일식 카테고리 식당 가중치
딸기 → 디저트/카페 카테고리 식당 가중치
```

### 3.5 관리자 식당 미디어: `restaurant_media`

식당 또는 메뉴별 이미지/동영상 노출에 사용한다.

| 컬럼 | 활용 |
|---|---|
| `restaurant_id` | 식당 대표 미디어 연결 |
| `menu_id` | 메뉴별 미디어 연결 |
| `media_type` | image/video 구분 |
| `usage_type` | representative/menu 구분 |
| `file_url` | 노출 이미지/영상 URL |

### 3.6 영상 테이블: `fp_300`

앱 홈/영상 콘텐츠의 집 단위 노출에 사용한다.

| 컬럼 | 활용 |
|---|---|
| `store_id` | 영상 콘텐츠 기준 집 식별자 |
| `title` | 영상 제목 텍스트 보조 매칭 |
| `store_name` | 집 이름 |
| `place_id` | 장소 연결 |
| `thumbnail` | 영상 썸네일 |
| `file_name` | 영상 파일 |

### 3.7 이미지 피드 테이블: `fp_400`

사용자 이미지 피드와 집/장소 연결에 사용한다.

| 컬럼 | 활용 |
|---|---|
| `feed_no` | 이미지 피드 ID |
| `feed_title` | 텍스트 보조 매칭 |
| `content` | 텍스트 보조 매칭 |
| `images` | 이미지 목록 |
| `thumbnail` | 대표 이미지 |
| `store_name` | 집 이름 |
| `place_id` | 장소 연결 |

---

## 4. 신규 테이블 제안

기존 `fp_330`~`fp_339`가 제철음식 본체라면, 아래 테이블은 앱 콘텐츠 매칭 계층이다.

DB에 과거 `fp_340`, `fp_341` 계열 테이블이 남아 있을 수 있으므로 실제 마이그레이션 전에는 충돌 여부를 확인한다.

### 4.1 `fp_340_season_match_keyword` - 제철 매칭 키워드 사전

제철 식재료를 메뉴/피드/영상 텍스트와 연결하기 위한 키워드 사전이다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `keyword_id` | `BIGSERIAL PK` | 키워드 ID |
| `ingredient_id` | `VARCHAR(30) NOT NULL` | `fp_333_ingredient.ingredient_id` |
| `keyword` | `VARCHAR(100) NOT NULL` | 매칭 키워드. 예: 굴국밥, 석화 |
| `keyword_type_cd` | `VARCHAR(50) NOT NULL` | BASE, ALIAS, DISH, MENU, EXCLUDE |
| `match_weight` | `NUMERIC(6,3) NOT NULL DEFAULT 1.0` | 매칭 가중치 |
| `exact_match_yn` | `CHAR(1) NOT NULL DEFAULT 'N'` | 정확히 일치해야 하는지 여부 |
| `description` | `TEXT` | 관리용 설명 |
| `use_yn` | `CHAR(1) NOT NULL DEFAULT 'Y'` | 사용 여부 |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | 생성일 |
| `updated_at` | `TIMESTAMPTZ` | 수정일 |

예시:

| ingredient_id | keyword | keyword_type_cd | match_weight |
|---|---|---|---:|
| ING_GUL | 굴 | BASE | 1.0 |
| ING_GUL | 생굴 | ALIAS | 1.2 |
| ING_GUL | 석화 | ALIAS | 1.2 |
| ING_GUL | 굴국밥 | DISH | 1.5 |
| ING_GUL | 굴전 | DISH | 1.5 |
| ING_GUL | 굴비 | EXCLUDE | 0 |
| ING_GUL | 동굴 | EXCLUDE | 0 |

### 4.2 `fp_341_season_store_match` - 제철음식 판매 집 매칭 결과

사용자 화면에서 직접 조회할 집 단위 캐시 테이블이다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `match_id` | `BIGSERIAL PK` | 매칭 결과 ID |
| `ingredient_id` | `VARCHAR(30) NOT NULL` | 제철 식재료 ID |
| `season_id` | `VARCHAR(30)` | 적용 가능한 제철 기간 ID |
| `store_id` | `INTEGER` | 앱 콘텐츠 기준 집 ID. `fp_300.store_id`, `fp_320.store_id` |
| `restaurant_id` | `BIGINT` | 관리자 식당 ID |
| `place_id` | `VARCHAR(255)` | Google place id 또는 장소 그룹 키 |
| `store_name` | `VARCHAR(150)` | 사용자 노출용 집 이름 |
| `address` | `VARCHAR(300)` | 사용자 노출용 주소 |
| `representative_menu_name` | `VARCHAR(150)` | 대표 매칭 메뉴명. 예: 굴국밥 |
| `representative_image_url` | `TEXT` | 대표 이미지 URL |
| `match_score` | `NUMERIC(8,3) NOT NULL DEFAULT 0` | 매칭 점수 |
| `season_score` | `SMALLINT` | 현재 월 제철 점수 |
| `evidence_count` | `INTEGER NOT NULL DEFAULT 0` | 근거 개수 |
| `match_status_cd` | `VARCHAR(50) NOT NULL DEFAULT 'AUTO'` | AUTO, CONFIRMED, REJECTED |
| `match_source_cd` | `VARCHAR(50) NOT NULL` | FP320_MENU, RESTAURANT_MENU, FEED, VIDEO, MIXED |
| `matched_keywords` | `VARCHAR(500)` | 매칭된 키워드 목록 |
| `reason_text` | `VARCHAR(300)` | 사용자에게 보여줄 짧은 이유 |
| `use_yn` | `CHAR(1) NOT NULL DEFAULT 'Y'` | 사용 여부 |
| `generated_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | 생성/갱신 시각 |
| `expires_at` | `TIMESTAMPTZ` | 캐시 만료 시각 |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | 최초 생성일 |
| `updated_at` | `TIMESTAMPTZ` | 수정일 |

권장 유니크 키:

```sql
UNIQUE (ingredient_id, COALESCE(store_id, -1), COALESCE(restaurant_id, -1), COALESCE(place_id, ''))
```

PostgreSQL에서는 표현식 유니크 인덱스로 구성한다.

### 4.3 `fp_342_season_match_evidence` - 매칭 근거

집 단위 매칭 결과가 어떤 원본 텍스트에서 나왔는지 추적한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `evidence_id` | `BIGSERIAL PK` | 근거 ID |
| `match_id` | `BIGINT NOT NULL` | `fp_341_season_store_match.match_id` |
| `target_type_cd` | `VARCHAR(50) NOT NULL` | FP320_MENU, RESTAURANT_MENU, FEED, VIDEO |
| `target_id` | `VARCHAR(100) NOT NULL` | 원본 데이터 ID. 예: item_id, menu_id, feed_no, store_id |
| `matched_field` | `VARCHAR(50) NOT NULL` | item_name, menu_title, description, content 등 |
| `matched_keyword` | `VARCHAR(100) NOT NULL` | 매칭된 키워드 |
| `matched_text` | `TEXT` | 매칭 원문 일부 |
| `field_weight` | `NUMERIC(6,3) NOT NULL DEFAULT 1.0` | 필드 가중치 |
| `keyword_weight` | `NUMERIC(6,3) NOT NULL DEFAULT 1.0` | 키워드 가중치 |
| `score` | `NUMERIC(8,3) NOT NULL DEFAULT 0` | 근거 점수 |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | 생성일 |

### 4.4 `fp_343_season_store_match_override` - 관리자 보정

자동 매칭 결과를 운영자가 확정/제외/대표 메뉴 지정할 수 있게 하는 테이블이다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `override_id` | `BIGSERIAL PK` | 보정 ID |
| `ingredient_id` | `VARCHAR(30) NOT NULL` | 제철 식재료 ID |
| `store_id` | `INTEGER` | 앱 콘텐츠 기준 집 ID |
| `restaurant_id` | `BIGINT` | 관리자 식당 ID |
| `place_id` | `VARCHAR(255)` | 장소 ID |
| `override_status_cd` | `VARCHAR(50) NOT NULL` | CONFIRMED, REJECTED, PINNED |
| `representative_menu_name` | `VARCHAR(150)` | 운영자가 지정한 대표 메뉴명 |
| `representative_image_url` | `TEXT` | 운영자가 지정한 대표 이미지 |
| `note` | `TEXT` | 운영자 메모 |
| `created_by` | `VARCHAR(100)` | 처리자 |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | 생성일 |
| `updated_at` | `TIMESTAMPTZ` | 수정일 |

---

## 5. 매칭 대상 우선순위

### 5.1 우선순위

| 순위 | 대상 | 이유 |
|---:|---|---|
| 1 | `fp_320.item_name`, `fp_320.menu_title` | 앱 콘텐츠와 바로 연결되는 메뉴명 |
| 2 | `restaurant_menus.name` | 관리자 검수 식당 메뉴명 |
| 3 | `fp_320.description`, `restaurant_menus.description` | 메뉴 설명 보조 근거 |
| 4 | `fp_400.feed_title`, `fp_400.content` | 사용자 피드 보조 근거 |
| 5 | `fp_300.title`, `fp_300.store_name` | 영상 제목/집 이름 보조 근거 |
| 6 | `restaurant_categories.category_code` | 큰 분류 보정용 |

### 5.2 점수 예시

```text
메뉴명 정확 포함: +100
메뉴명 별칭 포함: +90
메뉴 설명 포함: +50
피드 제목 포함: +40
피드 본문 포함: +25
영상 제목 포함: +25
식당 카테고리 관련성: +10
제외어 포함: 매칭 제외
관리자 확정: +1000 또는 상태 CONFIRMED
관리자 제외: 노출 제외
```

예:

```text
ingredient_id = ING_GUL
키워드 = 굴국밥
fp_320.item_name = "통영 굴국밥"

근거:
- target_type_cd = FP320_MENU
- matched_field = item_name
- matched_keyword = 굴국밥
- score = 100 * 1.5

집 결과:
- store_id = fp_320.store_id
- place_id = fp_320.place_id
- representative_menu_name = 통영 굴국밥
- reason_text = 지금 제철인 굴 메뉴가 있어요
```

---

## 6. 매칭 프로세스

### 6.1 배치 기반 추천

홈/지도/추천에서 사용할 데이터는 배치로 미리 만든다.

```text
1. 현재 월 기준 제철 식재료 조회
2. 식재료별 키워드 사전 조회
3. 제외어 사전 조회
4. fp_320 메뉴 텍스트 매칭
5. restaurant_menus 텍스트 매칭
6. fp_400 피드/이미지 텍스트 보조 매칭
7. fp_300 영상 텍스트 보조 매칭
8. store_id / restaurant_id / place_id 기준 집 단위로 병합
9. match_score, evidence_count, 대표 메뉴, 대표 이미지 계산
10. 관리자 보정값 반영
11. fp_341 / fp_342 갱신
```

### 6.2 저장/수정 시점 실시간 갱신

메뉴나 피드가 새로 등록될 때 해당 텍스트만 부분 재매칭할 수 있다.

대상 이벤트:

```text
fp_320 메뉴 등록/수정
restaurant_menus 등록/수정
fp_400 피드 등록/수정
fp_300 영상 등록/수정
제철 키워드 사전 변경
관리자 보정 변경
```

초기에는 일 배치 또는 수동 배치로 충분하고, 트래픽/운영 니즈가 생기면 이벤트 기반 갱신을 추가한다.

---

## 7. 조회 API 제안

### 7.1 이번 달 제철음식 파는 집 목록

```http
GET /api/season-foods/{ingredientId}/stores?month=12&lat=37.5&lng=127.0&radiusM=3000&page=0&size=20
```

응답 예시:

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
        "distanceM": 820
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

### 7.2 내 주변 제철 메뉴 집

```http
GET /api/season-stores/nearby?month=12&lat=37.5&lng=127.0&radiusM=3000
```

여러 제철 식재료를 섞어서 주변 집을 보여준다.

정렬 기준:

```text
거리 가까움
현재 월 season_score 높음
match_score 높음
관리자 확정 우선
이미지/영상 근거 있음 우선
```

### 7.3 집 상세의 제철 메뉴 배지

```http
GET /api/stores/{storeId}/season-foods?month=12
```

응답 예시:

```json
{
  "success": true,
  "data": [
    {
      "ingredientId": "ING_GUL",
      "ingredientName": "굴",
      "representativeMenuName": "굴전",
      "seasonScore": 95,
      "reasonText": "12월은 굴이 살이 오르고 맛이 좋아지는 시기예요"
    }
  ]
}
```

---

## 8. 홈/지도/검색 적용

### 8.1 홈

홈에서는 제철 식재료 카드보다 "제철음식이 있는 집" 카드가 앱 방향과 잘 맞는다.

예:

```text
지금 굴 먹기 좋은 집
제철 방어회 파는 곳
딸기 디저트가 있는 카페
```

홈 정렬에는 `fp_341_season_store_match.match_score`와 기존 추천 점수를 함께 사용할 수 있다.

### 8.2 지도

지도 마커에는 제철 메뉴 배지를 붙인다.

예:

```text
굴국밥
석화
방어회
딸기케이크
```

근처 검색은 `place_id` 또는 좌표를 기준으로 기존 지도 검색 결과와 `fp_341`을 조인한다.

### 8.3 검색

사용자가 `굴`을 검색하면 다음 순서로 결과를 구성한다.

```text
1. 굴 제철음식 정보
2. 굴 메뉴가 있는 집
3. 굴 관련 이미지/영상 피드
4. 굴 활용 레시피
```

---

## 9. 예외 처리

### 9.1 제외어

단순 포함 검색은 오탐이 발생한다.

예:

```text
굴 → 굴비, 동굴, 굴착, 굴러
배 → 배달, 배민, 배고픈
전어 → 전어가 아닌 "전 어..." 같은 띄어쓰기 문장
```

따라서 `fp_340_season_match_keyword.keyword_type_cd = EXCLUDE`로 제외어를 관리한다.

### 9.2 별칭

대표 식재료명과 실제 메뉴명이 다를 수 있다.

예:

```text
굴 → 석화, 생굴, 각굴
방어 → 대방어, 방어회
참외 → 참외화채
딸기 → 산딸기, 딸기케이크, 딸기라떼
```

별칭은 `ALIAS`, 실제 요리명은 `DISH` 또는 `MENU`로 관리한다.

### 9.3 계절성 없는 상시 메뉴

굴국밥처럼 상시 판매되는 메뉴라도 현재 굴이 제철이면 노출할 수 있다.

다만 사용자 문구는 "이 집은 굴국밥을 판매해요"와 "지금 굴이 제철이에요"를 분리해서 과장하지 않는다.

---

## 10. 운영 플로우

초기 운영은 다음 단계가 현실적이다.

```text
1. 제철 식재료 마스터 등록
2. 식재료별 키워드/별칭/제외어 등록
3. 메뉴/피드/영상 자동 매칭 배치 실행
4. 관리자 화면에서 상위 후보 검수
5. CONFIRMED/REJECTED 보정
6. 홈/지도/검색에 노출
```

관리자 화면에서 필요한 기능:

```text
제철 식재료별 매칭 후보 목록
매칭된 원문 확인
대표 메뉴명 수정
대표 이미지 수정
확정/제외 처리
키워드 추가
제외어 추가
```

---

## 11. 권장 구현 순서

### 1단계: 데이터 구조

```text
fp_340_season_match_keyword
fp_341_season_store_match
fp_342_season_match_evidence
fp_343_season_store_match_override
```

### 2단계: 자동 매칭 배치

```text
현재 월 제철 식재료 조회
키워드 사전 기반 fp_320 메뉴 매칭
restaurant_menus 매칭
매칭 결과 fp_341/fp_342 저장
```

### 3단계: 조회 API

```text
GET /api/season-foods/{ingredientId}/stores
GET /api/season-stores/nearby
GET /api/stores/{storeId}/season-foods
```

### 4단계: 보조 콘텐츠 연결

```text
fp_400 이미지 피드 연결
fp_300 영상 연결
대표 이미지/영상 선택 로직
```

### 5단계: 관리자 보정

```text
자동 매칭 후보 검수
확정/제외/대표 메뉴 지정
키워드 사전 관리
```

---

## 12. 결론

우리 앱에서는 제철음식 자체를 독립 콘텐츠로만 보여주기보다, 제철음식을 판매하거나 다루는 "집"을 찾게 만드는 구조가 더 적합하다.

이를 위해 기존 `fp_320` 메뉴 테이블을 1차 매칭 대상으로 삼고, 관리자 식당 메뉴, 이미지 피드, 영상 콘텐츠를 보조 근거로 활용한다.

핵심 신규 계층은 다음 두 가지다.

```text
제철 키워드 사전
제철음식 판매 집 매칭 결과 캐시
```

이 구조를 사용하면 다음 사용자 경험을 만들 수 있다.

```text
지금 굴이 제철이에요
근처에 굴국밥/석화 파는 집이 있어요
이 집에는 제철 굴 메뉴와 이미지가 있어요
```
