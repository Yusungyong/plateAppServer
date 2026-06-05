# Home Impression API Frontend Request

## 목적

홈 화면에서 실제 사용자 시야에 노출된 영상/이미지 항목을 서버에 저장합니다.
서버는 저장된 노출 이력을 기준으로 최근 48시간 동안 같은 홈 화면 항목이 다시 내려가지 않도록 필터링합니다.

## 서버 반영 사항

- 신규 API: `POST /api/home/impressions`
- 신규 테이블: `fp_376`
- 최근 48시간 노출 제외 적용 API:
  - `GET /api/home/video-thumbnails`
  - `GET /api/home/image-thumbnails`
  - `GET /api/home/content-feed`
  - `GET /api/home/search/content`
  - `GET /api/home/random-candidates/recent`
  - `GET /api/home/random-candidates/nearby`

DB 적용 SQL은 `docs/sql/home_impressions_fp376.sql`에 있습니다. 서버 설정이 `ddl-auto: validate`라서 배포 전에 해당 SQL이 먼저 적용되어야 합니다.

## 프론트에서 해야 할 일

홈 카드가 사용자 viewport에 실제로 보였을 때 `POST /api/home/impressions`로 배치 전송해주세요.

권장 기준:

- 카드 면적의 50% 이상이 1초 이상 보이면 노출로 판단
- 같은 렌더링 세션 안에서는 같은 항목을 중복 전송하지 않기
- 5-20개 단위 또는 2-3초 단위로 묶어서 배치 전송
- 로그인 사용자는 `Authorization: Bearer ...` 헤더 포함
- 게스트는 `isGuest: true`와 안정적인 `guestId` 포함

## 요청

```http
POST /api/home/impressions
Content-Type: application/json
Authorization: Bearer {accessToken}
```

게스트는 Authorization 없이 아래처럼 `guestId`를 보내면 됩니다.

```json
{
  "surface": "home",
  "requestId": "home-content-...",
  "isGuest": false,
  "guestId": null,
  "sessionId": "optional-session-id",
  "deviceId": "optional-device-id",
  "items": [
    {
      "contentType": "VIDEO",
      "storeId": 123,
      "positionNo": 0,
      "clientImpressedAt": "2026-06-05T10:15:30"
    },
    {
      "contentType": "IMAGE",
      "feedNo": 456,
      "positionNo": 1,
      "clientImpressedAt": "2026-06-05T10:15:31"
    }
  ]
}
```

## 필드 매핑

`contentType`

- 영상: `VIDEO`
- 이미지: `IMAGE`

`storeId`

- `VIDEO`일 때 필수
- `/api/home/video-thumbnails` 응답의 `storeId`
- `/api/home/content-feed` 응답 중 `contentType=VIDEO`인 항목의 `storeId`

`feedNo`

- `IMAGE`일 때 필수
- `/api/home/image-thumbnails` 응답의 `feedNo`
- `/api/home/content-feed` 응답 중 `contentType=IMAGE`인 항목의 `imageFeedId`를 `feedNo`로 보내기

`requestId`

- `/api/home/content-feed`: 응답의 `trackingToken`
- `/api/home/video-thumbnails`: 항목의 `recommendationRequestId`
- `/api/home/image-thumbnails`: 현재 응답에 별도 request id가 없으므로 생략 가능

## 응답

```json
{
  "success": true,
  "data": {
    "savedCount": 2,
    "duplicateCount": 0,
    "suppressUntil": "2026-06-07T10:15:32"
  }
}
```

## 주의사항

- 서버는 사용자가 실제로 봤는지 알 수 없으므로, viewport 노출 판단은 프론트에서 해야 합니다.
- 로그인 사용자는 가능하면 username을 직접 보내지 말고 Authorization 헤더로 식별해주세요.
- 게스트는 같은 기기/앱 설치 단위로 유지되는 `guestId`가 있어야 48시간 제외가 동작합니다.
- `POST /api/home/video-events`는 영상 추천 학습용 이벤트이고, 이 문서의 impression API는 홈 재노출 방지용입니다.
