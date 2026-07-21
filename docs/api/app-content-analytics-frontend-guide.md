# 앱 콘텐츠 통계 API 프론트 연동 가이드

- 문서 버전: `v1.0`
- 작성일: `2026-07-21`
- 대상: 일반 앱 사용자가 자신이 게시한 영상·이미지 콘텐츠의 성과를 확인하는 화면
- 인증: 로그인 사용자 본인 전용

> 상태: `/api/my/content-analytics/**` 3개 API는 서버 구현과 자동 테스트가 완료되었다. 실제 운영 호출은 이 서버 변경이 배포된 뒤 시작한다.

## 1. 연동 범위

프론트 콘텐츠 통계 화면은 다음 세 API를 기준으로 구성한다.

| 용도 | Method | Endpoint | 상태 |
|---|---|---|---|
| 상단 요약 카드 | `GET` | `/api/my/content-analytics/summary` | 서버 구현 완료, 배포 필요 |
| 일별 추이 그래프 | `GET` | `/api/my/content-analytics/trends` | 서버 구현 완료, 배포 필요 |
| 콘텐츠별 성과 목록 | `GET` | `/api/my/content-analytics/contents` | 서버 구현 완료, 배포 필요 |

초기 버전에서는 별도 이미지 조회 이벤트가 없으므로 이미지 게시물의 조회수, 체류시간, 공유 수는 제공하지 않는다. 이미지 항목의 영상 전용 묶음은 `videoMetrics: null`로 응답한다.

## 2. 공통 호출 규칙

### Base URL

환경별 API 주소를 사용한다.

```text
${API_BASE_URL}
```

예시:

```text
https://api.example.com/api/my/content-analytics/summary
```

### 인증 헤더

모든 콘텐츠 통계 API에는 액세스 토큰이 필요하다.

```http
Authorization: Bearer {accessToken}
Accept: application/json
```

사용자 이름은 query 또는 path로 전달하지 않는다. 서버가 JWT의 로그인 사용자를 기준으로 본인 콘텐츠만 집계한다.

### 기간 규칙

- `from`, `to` 형식: `YYYY-MM-DD`
- 두 날짜 모두 필수
- `from`과 `to`는 한국 시간(`Asia/Seoul`) 기준
- `to` 날짜를 포함한다.
- 기간은 **게시물 작성 기간이 아니라 성과 이벤트 발생 기간**에 적용한다.
- 콘텐츠 수는 게시일과 관계없이 조회 시점에 존재하는 본인의 활성 콘텐츠를 집계한다.
- 최초 권장 조회 기간은 최근 30일이다.
- 추이 조회 최대 기간은 93일로 제한한다.

서버는 `Asia/Seoul` 날짜 경계인 `[from 00:00, to + 1일 00:00)`를 사용한다. `timestamp without time zone`인 기존 이벤트 시각은 한국 시간 wall-clock 값으로 저장된다는 현재 데이터 규약을 따른다. `fp_305.timestamp`처럼 offset이 있는 시각은 같은 경계를 offset 시각으로 변환해 조회한다.

예시:

```text
from=2026-07-01&to=2026-07-21
```

### 공통 성공 응답

```json
{
  "success": true,
  "data": {},
  "requestId": "c7b04d5d5f0f4d1b",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

실제 화면 데이터는 항상 `data` 안에서 읽는다.

### 공통 실패 응답

```json
{
  "success": false,
  "message": "요청 값이 올바르지 않습니다.",
  "errorCode": "COMMON_400",
  "requestId": "c7b04d5d5f0f4d1b",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

| HTTP 상태 | 프론트 처리 |
|---|---|
| `400` | 날짜, 정렬값 등 요청값 확인 메시지 표시 |
| `401` | 토큰 갱신 후 한 번 재호출, 실패하면 로그인 화면 이동 |
| `403` | 접근 권한 없음 안내 |
| `404` | 삭제되었거나 존재하지 않는 콘텐츠 안내 |
| `429` | 잠시 후 재시도 안내 |
| `500` | 공통 오류 화면과 `requestId` 기록 |

요청값 오류의 실제 `errorCode`는 다음과 같다. HTTP 상태를 우선 기준으로 처리하고, `COMMON_401`을 인증 실패로 오인하지 않는다.

| 상황 | HTTP 상태 | `errorCode` |
|---|---:|---|
| `from` 또는 `to` 누락 | `400` | `COMMON_401` |
| 날짜·정수 형식 불일치 | `400` | `COMMON_402` |
| 날짜 범위, `type`, `sort`, `interval`, `page`, `size` 값 오류 | `400` | `COMMON_400` |
| 미인증 또는 만료된 인증 | `401` | `AUTH_401` 또는 인증 계열 코드 |

## 3. 요약 통계

### 요청

```http
GET /api/my/content-analytics/summary?from=2026-07-01&to=2026-07-21
Authorization: Bearer {accessToken}
```

### Query parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `from` | `string(date)` | O | 조회 시작일 |
| `to` | `string(date)` | O | 조회 종료일, 해당 날짜 포함 |

### 응답 예시

```json
{
  "success": true,
  "data": {
    "period": {
      "from": "2026-07-01",
      "to": "2026-07-21",
      "timezone": "Asia/Seoul"
    },
    "content": {
      "totalCount": 12,
      "videoCount": 5,
      "imageCount": 7,
      "publicCount": 10,
      "privateCount": 1,
      "unknownVisibilityCount": 1
    },
    "exposure": {
      "impressionCount": 4200,
      "uniqueAudienceCount": 1800
    },
    "engagement": {
      "activeReceivedLikeCount": 340,
      "periodNewActiveLikeCount": 42,
      "periodCommentCount": 52,
      "periodReplyCount": 18,
      "periodShareCount": 21,
      "engagementRate": 0.0317
    },
    "video": {
      "watchSessionCount": 1200,
      "uniqueViewerCount": 790,
      "totalWatchSeconds": 25800,
      "averageWatchSeconds": 21.5,
      "completedViewCount": 430,
      "completionRate": 0.3583
    }
  },
  "requestId": "c7b04d5d5f0f4d1b",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

### 화면 표시 기준

- `content.totalCount`: 전체 게시물 카드
- `exposure.impressionCount`: 총 노출 카드
- `engagement.activeReceivedLikeCount`: 받은 좋아요 카드
- `engagement.periodCommentCount + periodReplyCount`: 기간 내 댓글 카드
- `video.watchSessionCount`: 영상 조회 카드
- `video.completionRate`: 완주율 카드

`completionRate`는 `0.0 ~ 1.0` 범위다. `engagementRate`는 `0.0` 이상이며 한 번의 노출에서 여러 반응이 생길 수 있어 `1.0`을 넘을 수 있다. 두 값 모두 프론트에서 퍼센트로 표시할 때 `100`을 곱한다.

```ts
const formatRate = (rate: number | null) =>
  rate == null ? "-" : `${(rate * 100).toFixed(1)}%`;
```

## 4. 일별 추이

### 요청

```http
GET /api/my/content-analytics/trends?from=2026-07-01&to=2026-07-21&interval=day
Authorization: Bearer {accessToken}
```

### Query parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---:|---|---|
| `from` | `string(date)` | O | - | 조회 시작일 |
| `to` | `string(date)` | O | - | 조회 종료일 |
| `interval` | `string` | X | `day` | 초기 버전은 `day`만 지원 |

### 응답 예시

```json
{
  "success": true,
  "data": {
    "period": {
      "from": "2026-07-01",
      "to": "2026-07-21",
      "timezone": "Asia/Seoul"
    },
    "interval": "day",
    "points": [
      {
        "date": "2026-07-01",
        "impressionCount": 180,
        "uniqueAudienceCount": 95,
        "videoPlayStartCount": 74,
        "videoCompleteCount": 21,
        "watchSeconds": 1510,
        "newActiveLikeCount": 6,
        "commentCount": 3,
        "replyCount": 1,
        "shareCount": 2
      },
      {
        "date": "2026-07-02",
        "impressionCount": 205,
        "uniqueAudienceCount": 111,
        "videoPlayStartCount": 88,
        "videoCompleteCount": 30,
        "watchSeconds": 1920,
        "newActiveLikeCount": 9,
        "commentCount": 4,
        "replyCount": 2,
        "shareCount": 1
      }
    ]
  },
  "requestId": "fa7f95028462498b",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

조회 기간 내 데이터가 없는 날짜도 `0` 값으로 포함한다. 프론트가 날짜 배열을 별도로 채우지 않아도 그래프가 연속적으로 표시되어야 한다.

추천 그래프:

- 노출: `impressionCount`
- 재생: `videoPlayStartCount`
- 반응: `newActiveLikeCount + commentCount + replyCount + shareCount`
- 완주: `videoCompleteCount`

## 5. 콘텐츠별 성과 목록

### 요청

```http
GET /api/my/content-analytics/contents?from=2026-07-01&to=2026-07-21&type=all&sort=impressions&page=0&size=20
Authorization: Bearer {accessToken}
```

### Query parameters

| 이름 | 타입 | 필수 | 기본값 | 허용값/설명 |
|---|---|---:|---|---|
| `from` | `string(date)` | O | - | 조회 시작일 |
| `to` | `string(date)` | O | - | 조회 종료일 |
| `type` | `string` | X | `all` | `all`, `video`, `image` |
| `sort` | `string` | X | `impressions` | `impressions`, `views`, `likes`, `comments`, `recent` |
| `page` | `integer` | X | `0` | 0부터 시작 |
| `size` | `integer` | X | `20` | `1 ~ 100` |

`sort=views`에서 이미지 콘텐츠는 실제 조회수가 없으므로 정렬 내부 값을 미지원(`null`)으로 취급하고 영상 뒤에 배치한다.

콘텐츠 목록에는 게시일과 관계없이 조회 시점에 존재하는 본인의 활성 콘텐츠를 포함하고, 각 콘텐츠의 `from ~ to` 기간 성과를 붙인다. 정렬 기준은 다음과 같다.

| 정렬값 | 기준 |
|---|---|
| `impressions` | 기간 내 `impressionCount` 내림차순 |
| `views` | 기간 내 영상 `watchSessionCount` 내림차순. 이미지는 마지막에 배치 |
| `likes` | 기간 내 `periodNewActiveLikeCount` 내림차순 |
| `comments` | 기간 내 `periodCommentCount + periodReplyCount` 내림차순 |
| `recent` | `publishedOn` 내림차순 |

`visibility=UNKNOWN`은 영상 또는 이미지의 기존 `open_yn` 값이 없거나 `Y`/`N`이 아니어서 공개 여부를 확정할 수 없는 경우다. 백엔드 데이터 정리가 끝나기 전에는 프론트에서 임의로 `PUBLIC` 처리하지 않는다.

`publishedOn`은 레거시 콘텐츠의 작성일이 비어 있으면 `null`일 수 있다. `sort=recent`에서는 이런 항목을 마지막에 배치한다.

### 응답 예시

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "contentType": "VIDEO",
        "contentId": "video:301",
        "videoStoreId": 301,
        "imageFeedId": null,
        "title": "을지로 파스타 후기",
        "thumbnailUrl": "https://cdn.example.com/video/301.jpg",
        "publishedOn": "2026-07-10",
        "visibility": "PUBLIC",
        "imageCount": null,
        "metrics": {
          "impressionCount": 1250,
          "uniqueAudienceCount": 710,
          "activeLikeCount": 96,
          "periodNewActiveLikeCount": 18,
          "periodCommentCount": 14,
          "periodReplyCount": 5,
          "engagementRate": 0.0384
        },
        "videoMetrics": {
          "watchSessionCount": 540,
          "uniqueViewerCount": 360,
          "totalWatchSeconds": 11400,
          "averageWatchSeconds": 21.1,
          "completedViewCount": 185,
          "completionRate": 0.3426,
          "shareCount": 11
        }
      },
      {
        "contentType": "IMAGE",
        "contentId": "image:802",
        "videoStoreId": null,
        "imageFeedId": 802,
        "title": "성수동 디저트 모음",
        "thumbnailUrl": "https://cdn.example.com/feed/802.jpg",
        "publishedOn": "2026-07-08",
        "visibility": "PUBLIC",
        "imageCount": 4,
        "metrics": {
          "impressionCount": 830,
          "uniqueAudienceCount": 510,
          "activeLikeCount": 72,
          "periodNewActiveLikeCount": 12,
          "periodCommentCount": 9,
          "periodReplyCount": 3,
          "engagementRate": 0.0289
        },
        "videoMetrics": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1,
    "hasNext": false
  },
  "requestId": "0d03bd5a640d43a4",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

### 타입별 렌더링

```ts
if (item.contentType === "VIDEO" && item.videoMetrics) {
  // 조회수, 평균 시청시간, 완주율, 공유 수 표시
} else {
  // 이미지 수, 노출, 좋아요, 댓글 표시
}
```

이미지 콘텐츠에는 `videoMetrics: null`을 내려준다. 프론트는 이미지 조회수를 `0회`로 표시하지 말고 해당 항목을 숨긴다.

## 6. TypeScript 타입 예시

```ts
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  requestId?: string;
  timestamp: string;
}

export interface AnalyticsPeriod {
  from: string;
  to: string;
  timezone: "Asia/Seoul";
}

export interface ContentAnalyticsSummary {
  period: AnalyticsPeriod;
  content: {
    totalCount: number;
    videoCount: number;
    imageCount: number;
    publicCount: number;
    privateCount: number;
    unknownVisibilityCount: number;
  };
  exposure: {
    impressionCount: number;
    uniqueAudienceCount: number;
  };
  engagement: {
    activeReceivedLikeCount: number;
    periodNewActiveLikeCount: number;
    periodCommentCount: number;
    periodReplyCount: number;
    periodShareCount: number;
    engagementRate: number | null;
  };
  video: {
    watchSessionCount: number;
    uniqueViewerCount: number;
    totalWatchSeconds: number;
    averageWatchSeconds: number | null;
    completedViewCount: number;
    completionRate: number | null;
  };
}

export interface AnalyticsTrendPoint {
  date: string;
  impressionCount: number;
  uniqueAudienceCount: number;
  videoPlayStartCount: number;
  videoCompleteCount: number;
  watchSeconds: number;
  newActiveLikeCount: number;
  commentCount: number;
  replyCount: number;
  shareCount: number;
}

export interface ContentAnalyticsTrends {
  period: AnalyticsPeriod;
  interval: "day";
  points: AnalyticsTrendPoint[];
}

export type ContentType = "VIDEO" | "IMAGE";
export type ContentVisibility = "PUBLIC" | "PRIVATE" | "UNKNOWN";

export interface ContentAnalyticsItem {
  contentType: ContentType;
  contentId: string;
  videoStoreId: number | null;
  imageFeedId: number | null;
  title: string;
  thumbnailUrl: string | null;
  publishedOn: string | null;
  visibility: ContentVisibility;
  imageCount: number | null;
  metrics: {
    impressionCount: number;
    uniqueAudienceCount: number;
    activeLikeCount: number;
    periodNewActiveLikeCount: number;
    periodCommentCount: number;
    periodReplyCount: number;
    engagementRate: number | null;
  };
  videoMetrics: {
    watchSessionCount: number;
    uniqueViewerCount: number;
    totalWatchSeconds: number;
    averageWatchSeconds: number | null;
    completedViewCount: number;
    completionRate: number | null;
    shareCount: number;
  } | null;
}

export interface ContentAnalyticsPage {
  content: ContentAnalyticsItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
```

## 7. Axios 호출 예시

```ts
import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

export async function getContentAnalyticsSummary(
  from: string,
  to: string,
  signal?: AbortSignal,
) {
  const response = await api.get<ApiResponse<ContentAnalyticsSummary>>(
    "/api/my/content-analytics/summary",
    {
      params: { from, to },
      signal,
    },
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message ?? "통계를 불러오지 못했습니다.");
  }

  return response.data.data;
}

export async function getContentAnalyticsTrends(
  from: string,
  to: string,
  signal?: AbortSignal,
) {
  const response = await api.get<ApiResponse<ContentAnalyticsTrends>>(
    "/api/my/content-analytics/trends",
    {
      params: { from, to, interval: "day" },
      signal,
    },
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message ?? "통계 추이를 불러오지 못했습니다.");
  }

  return response.data.data;
}

export async function getContentAnalyticsContents(params: {
  from: string;
  to: string;
  type?: "all" | "video" | "image";
  sort?: "impressions" | "views" | "likes" | "comments" | "recent";
  page?: number;
  size?: number;
  signal?: AbortSignal;
}) {
  const { signal, ...query } = params;
  const response = await api.get<ApiResponse<ContentAnalyticsPage>>(
    "/api/my/content-analytics/contents",
    {
      params: query,
      signal,
    },
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message ?? "콘텐츠별 통계를 불러오지 못했습니다.");
  }

  return response.data.data;
}
```

화면 전환이나 기간 변경 시 이전 요청은 `AbortController`로 취소한다.

## 8. 지표 정의

| 필드 | 정의 |
|---|---|
| `impressionCount` | 앱이 서버에 기록한 홈 콘텐츠 노출 횟수. 콘텐츠 통계에서는 `fp_376`만 원천으로 사용 |
| `uniqueAudienceCount` | 노출 행에서 `username → guestId → deviceId → sessionId` 순서로 사용 가능한 식별자를 선택해 중복 제거한 사용자 수 |
| `watchSessionCount` | `fp_305`에 영상 시청 시작으로 생성된 유효 시청 세션 수 |
| `videoPlayStartCount` | `fp_370`의 `PLAY_START` 이벤트 수. 게스트 이벤트를 포함할 수 있어 `watchSessionCount`와 같지 않을 수 있음 |
| `uniqueViewerCount` | 시청 세션의 로그인 사용자 기준 고유 시청자 수 |
| `activeReceivedLikeCount` | 현재 활성 상태로 남아 있는 받은 좋아요 수. 작성자 본인의 좋아요는 제외 |
| `periodNewActiveLikeCount` | 기간 내 마지막으로 활성화되었고 현재도 활성인 받은 좋아요 수. 영상은 활성 행의 `updated_at`(없으면 `created_at`), 이미지는 현재 행의 `created_at`을 활성화 시각으로 사용 |
| `periodCommentCount` | 조회 기간에 생성된 현재 활성 최상위 댓글 수. 작성자 본인의 댓글은 제외 |
| `periodReplyCount` | 조회 기간에 생성된 현재 활성 답글 수. 작성자 본인의 답글은 제외 |
| `completionRate` | `completedViewCount / watchSessionCount` |
| `engagementRate` | 기간 내 작성자 본인 반응을 제외한 좋아요·댓글·답글·공유 합계 `/ impressionCount` |

분모가 `0`인 비율은 `0`이 아니라 `null`로 응답한다.
`engagementRate`는 반응 합계가 노출 수보다 많으면 `1.0`을 넘을 수 있다.

### 집계 원천 고정 규칙

- 노출은 영상·이미지 모두 `fp_376`을 기준으로 한다. 영상의 `fp_370 IMPRESSION`을 더하지 않는다.
- 영상 시청 세션·시청시간·완주는 `fp_305`를 기준으로 한다.
- 영상 클릭·재생 시작·공유 등 퍼널 행동은 `fp_370`을 기준으로 한다.
- `PLAY_PROGRESS` 이벤트 건수는 조회수나 시청 세션 수로 사용하지 않는다.
- 좋아요·댓글·답글의 현재 상태는 실제 CRUD 테이블을 기준으로 하며, 클라이언트 행동 이벤트를 현재 반응 수에 합산하지 않는다.
- 동일 사용자의 본인 콘텐츠 좋아요·댓글·답글·공유는 작성자 성과에서 제외한다.

## 9. 현재 사용 가능한 API

통합 통계 API와 별개로 유지되는 기존 API다. 신규 화면은 배포 후 1~5장의 통합 API를 우선 사용하고, 아래 API를 통계 합산용으로 섞어 쓰지 않는다.

### 내 영상 목록과 반응 수

```http
GET /api/my/videos?limit=20&offset=0&sort=popular&from=2026-07-01&to=2026-07-21
Authorization: Bearer {accessToken}
```

공통 응답의 `data` 예시:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "storeId": 301,
        "title": "을지로 파스타 후기",
        "thumbnail": "https://cdn.example.com/301.jpg",
        "fileName": "https://cdn.example.com/301.mp4",
        "videoDuration": 43,
        "createdAt": "2026-07-10T00:00:00",
        "likeCount": 96,
        "commentCount": 14
      }
    ],
    "limit": 20,
    "offset": 0,
    "total": 5
  },
  "requestId": "7b8eb276f42b4d71",
  "timestamp": "2026-07-21T12:30:00Z"
}
```

### 내 이미지 목록과 반응 수

```http
GET /api/my/images?limit=20&offset=0&sort=popular&from=2026-07-01&to=2026-07-21
Authorization: Bearer {accessToken}
```

주요 응답값은 `feedId`, `title`, `thumbnail`, `imageCount`, `likeCount`, `commentCount`다.

기존 API의 조회 범위는 신규 계약과 다르다. `/api/my/videos`는 공개 영상만 반환하고, `/api/my/images`는 현재 공개 여부를 필터링하지 않는다. 따라서 두 기존 목록의 합계가 신규 summary의 공개·비공개·미확정 콘텐츠 수와 다를 수 있다.

### 개별 영상 시청 통계

```http
GET /api/videos/{storeId}/watch-stats
Authorization: Bearer {accessToken}
```

```json
{
  "success": true,
  "data": {
    "storeId": 301,
    "totalViews": 540,
    "uniqueViewers": 360,
    "averageDuration": 21.1,
    "completionRate": 0.3426,
    "completedViews": 185,
    "qualityDistribution": {
      "quality1080p": 110,
      "quality720p": 260,
      "quality360p": 95,
      "qualityAuto": 75
    },
    "deviceDistribution": {
      "ios": 230,
      "android": 270,
      "web": 25,
      "other": 15
    }
  }
}
```

현재 이 API에는 기간 조회와 작성자 소유권 검사가 없다. 작성자용 화면에 정식 적용하기 전 서버 보완이 필요하다.

### 마이페이지 허브 요약

```http
GET /api/my/hub?previewLimit=3
Authorization: Bearer {accessToken}
```

`contentCount`, `videoCount`, `imageCount`, `receivedLikeCount`, `friendCount` 등을 제공하지만 서버 기능 플래그 `MY_HUB_ENABLED`와 `MY_HUB_IMAGE_VISIBILITY_READY`가 모두 활성화되어야 한다. 기본 설정은 비활성화다.

## 10. 현재 데이터 한계

- 홈 노출은 앱이 `/api/home/impressions`를 실제 호출한 이후의 데이터만 집계된다.
- 영상 퍼널 이벤트도 앱이 `/api/home/video-events`를 정상 전송한 경우에만 집계된다.
- 이미지 상세 조회·체류·넘김·공유 이벤트는 현재 수집하지 않는다.
- 별도 저장 또는 북마크 기능은 없으며 현재 데이터는 `좋아요`다.
- 좋아요 취소와 댓글 삭제 후의 과거 활동은 정확히 복원할 수 없다.
- 영상 시청 통계의 고유 시청자는 로그인 사용자를 중심으로 계산되어 게스트 수치와 차이가 날 수 있다.
- `fp_305.timestamp`는 시청 진행·완료 갱신 때 마지막 활동 시각으로 바뀌므로, 기간별 시청 지표는 세션 시작일이 아니라 마지막 갱신일 기준의 근사치다.
- 홈 콘텐츠 응답의 기존 `viewCount`는 현재 `0`으로 고정되어 있으므로 통계 화면에 사용하지 않는다.
- 시청자 이름, IP, 기기 ID와 같은 개인 식별 정보는 프론트에 내려주지 않는다.

## 11. 프론트 구현 체크리스트

- [ ] 서버 변경의 운영 배포와 스모크 테스트가 끝난 뒤 신규 endpoint 호출을 활성화한다.
- [ ] 모든 요청에 Bearer 토큰을 전달한다.
- [ ] 기본 조회 기간을 최근 30일로 설정한다.
- [ ] 날짜 변경 시 summary, trends, contents를 같은 기간으로 다시 조회한다.
- [ ] `Rate` 값은 `100`을 곱해 퍼센트로 표시한다.
- [ ] `null` 지표는 `0`으로 바꾸지 않고 `-` 또는 미지원으로 표시한다.
- [ ] 이미지 콘텐츠에서는 영상 전용 지표를 숨긴다.
- [ ] 빈 목록과 모든 값이 0인 정상 응답을 오류로 처리하지 않는다.
- [ ] 실패 응답의 `requestId`를 오류 로그에 포함한다.
- [ ] 콘텐츠 목록의 `page`는 0부터 시작한다.
