# 관리 음식점 미디어 프론트 연동 가이드

## 목적

관리 화면에서 음식점 대표 이미지, 대표 동영상, 메뉴 이미지를 업로드하고 미리보기할 때 사용하는 API와 `fileUrl` 처리 방식을 설명합니다.

핵심 원칙:

- 프론트는 S3 또는 CDN 주소를 직접 조합하지 않습니다.
- 업로드 API와 상세 API가 반환한 `fileUrl`을 그대로 사용합니다.
- `fileUrl`은 브라우저가 직접 조회할 수 있는 CDN URL입니다.
- S3 원본 버킷은 비공개로 운영할 수 있으며, CloudFront가 S3 객체를 조회합니다.

## 전체 흐름

1. 프론트가 `POST /api/admin/files`로 파일을 업로드합니다.
2. 서버가 파일을 S3에 저장합니다.
3. 업로드 응답으로 CDN 기반 `fileUrl`을 반환합니다.
4. 프론트가 음식점 생성 또는 수정 요청의 `media[].fileUrl`에 해당 값을 그대로 넣습니다.
5. 서버는 DB에 S3 객체 키만 저장합니다.
6. 음식점 상세 조회 시 서버가 객체 키를 CDN URL로 변환하여 반환합니다.
7. 프론트는 상세 응답의 `fileUrl`을 `<img>` 또는 `<video>`의 `src`로 사용합니다.

예시:

```text
S3 object key
restaurants/2026-06-12/876a614996584fdbadc38cea865436d9/video.mp4

API fileUrl
https://{cloudfront-domain}/restaurants/2026-06-12/876a614996584fdbadc38cea865436d9/video.mp4
```

## 인증

관리 API 요청에는 관리자 Access Token이 필요합니다.

```http
Authorization: Bearer {accessToken}
```

미디어 `fileUrl` 조회 시에는 API Authorization 헤더를 붙이지 않습니다. 브라우저가 CDN URL을 직접 조회합니다.

## 파일 업로드

### 요청

```http
POST /api/admin/files
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

multipart 필드명은 반드시 `file`이어야 합니다.

```ts
async function uploadRestaurantMedia(file: File, accessToken: string) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch("/api/admin/files", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: formData,
  });

  const result = await response.json();

  if (!response.ok || !result.success) {
    throw new Error(result.message ?? "파일 업로드에 실패했습니다.");
  }

  return result.data;
}
```

`FormData`를 사용할 때 `Content-Type` 헤더를 직접 지정하지 마세요. 브라우저가 multipart boundary를 포함한 헤더를 자동 생성해야 합니다.

### 업로드 제한

| 종류 | 확장자 | 최대 크기 |
|---|---|---:|
| 이미지 | `jpg`, `jpeg`, `png`, `webp`, `gif` | 10MB |
| 동영상 | `mp4`, `mov`, `webm` | 100MB |

서버는 확장자에 따라 표준 MIME 타입을 S3 객체에 저장합니다.

| 확장자 | 응답 `mimeType` |
|---|---|
| `jpg`, `jpeg` | `image/jpeg` |
| `png` | `image/png` |
| `webp` | `image/webp` |
| `gif` | `image/gif` |
| `mp4` | `video/mp4` |
| `mov` | `video/quicktime` |
| `webm` | `video/webm` |

### 응답

```json
{
  "success": true,
  "data": {
    "fileUrl": "https://cdn.example.com/restaurants/2026-06-12/.../video.mp4",
    "originalName": "store-preview.mp4",
    "mimeType": "video/mp4",
    "fileSizeBytes": 5242880
  },
  "requestId": "request-id",
  "timestamp": "2026-06-12T09:00:00Z"
}
```

업로드 응답의 네 필드는 음식점 생성·수정 요청에 그대로 재사용할 수 있습니다.

## 음식점 저장

### 요청 예시

```http
POST /api/admin/restaurants
Content-Type: application/json
Authorization: Bearer {accessToken}
```

```json
{
  "title": "플레이트 키친",
  "address": "서울특별시 강남구 테헤란로 123",
  "phone": "02-1234-5678",
  "businessHours": "매일 11:00 - 22:00",
  "introduction": "음식점 소개",
  "exposureStatus": "draft",
  "categories": ["KOREAN"],
  "media": [
    {
      "mediaType": "video",
      "usageType": "representative",
      "fileUrl": "https://cdn.example.com/restaurants/2026-06-12/.../video.mp4",
      "originalName": "store-preview.mp4",
      "mimeType": "video/mp4",
      "fileSizeBytes": 5242880,
      "displayOrder": 0
    }
  ],
  "menus": [
    {
      "name": "대표 메뉴",
      "price": 12000,
      "description": "메뉴 설명",
      "displayOrder": 0,
      "media": [
        {
          "mediaType": "image",
          "usageType": "menu",
          "fileUrl": "https://cdn.example.com/restaurants/2026-06-12/.../menu.jpg",
          "originalName": "menu.jpg",
          "mimeType": "image/jpeg",
          "fileSizeBytes": 204800,
          "displayOrder": 0
        }
      ]
    }
  ]
}
```

필드 규칙:

| 위치 | `mediaType` | `usageType` |
|---|---|---|
| 최상위 `media[]` | `image` 또는 `video` | `representative` |
| `menus[].media[]` | `image` 또는 `video` | `menu` |

`fileUrl`은 업로드 API 응답값을 수정하지 않고 그대로 전달합니다.

수정은 같은 구조로 요청합니다.

```http
PUT /api/admin/restaurants/{restaurantId}
```

수정 요청에서 제외된 기존 미디어는 서버가 S3에서도 삭제합니다. 기존 미디어를 유지하려면 상세 조회로 받은 미디어 항목을 수정 요청에 다시 포함해야 합니다.

## 상세 조회와 프리뷰

### 요청

```http
GET /api/admin/restaurants/{restaurantId}
Authorization: Bearer {accessToken}
```

### 미디어 응답 예시

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "플레이트 키친",
    "media": [
      {
        "id": 10,
        "mediaType": "video",
        "usageType": "representative",
        "fileUrl": "https://cdn.example.com/restaurants/2026-06-12/.../video.mp4",
        "originalName": "store-preview.mp4",
        "mimeType": "video/mp4",
        "fileSizeBytes": 5242880,
        "displayOrder": 0
      }
    ],
    "menus": []
  }
}
```

### React 프리뷰 예시

```tsx
type RestaurantMedia = {
  id?: number;
  mediaType: "image" | "video";
  usageType: "representative" | "menu";
  fileUrl: string;
  originalName?: string | null;
  mimeType?: string | null;
};

export function MediaPreview({ media }: { media: RestaurantMedia }) {
  if (media.mediaType === "video") {
    return (
      <video
        src={media.fileUrl}
        controls
        preload="metadata"
        playsInline
        style={{ maxWidth: "100%" }}
      >
        브라우저가 동영상 재생을 지원하지 않습니다.
      </video>
    );
  }

  return (
    <img
      src={media.fileUrl}
      alt={media.originalName ?? "음식점 미디어"}
      loading="lazy"
      style={{ maxWidth: "100%" }}
    />
  );
}
```

동영상 프리뷰를 위해 파일 전체를 `fetch()`로 내려받아 Blob URL로 변환할 필요는 없습니다. `<video src={fileUrl}>`를 사용하면 브라우저가 필요한 구간을 Range 요청으로 조회합니다.

## 프론트 구현 주의사항

- S3 주소를 CDN 주소로 문자열 치환하지 마세요.
- CDN 도메인을 프론트 환경변수에 하드코딩하지 마세요.
- 업로드 응답과 상세 응답의 `fileUrl`을 그대로 사용하세요.
- 동영상에 API Authorization 헤더를 붙이기 위해 별도 `fetch()`를 사용하지 마세요.
- 수정 화면에서 유지할 기존 미디어도 PUT 요청에 반드시 포함하세요.
- `mediaType`은 파일 MIME만 보고 추론하지 말고 업로드 대상 UI의 종류와 함께 명시하세요.
- Object URL로 업로드 직후 임시 프리뷰를 만들었다면 컴포넌트 해제 시 `URL.revokeObjectURL()`을 호출하세요.

## 오류 확인

### 프리뷰 요청이 `403 Forbidden`

CDN이 아닌 비공개 S3 URL을 받았거나 CloudFront 원본 접근 설정이 잘못된 상태입니다. 상세 응답의 `fileUrl` 도메인을 먼저 확인하세요.

### 동영상 탐색이 되지 않음

브라우저 개발자 도구 Network 탭에서 동영상 요청을 확인합니다.

정상적인 Range 응답 예시:

```http
HTTP/1.1 206 Partial Content
Content-Type: video/mp4
Accept-Ranges: bytes
Content-Range: bytes 0-1048575/5242880
```

### 브라우저 CORS 오류

CDN 응답에 관리 프론트 Origin을 허용하는 `Access-Control-Allow-Origin` 헤더가 있어야 합니다. 이 설정은 프론트 코드가 아니라 CloudFront 응답 헤더 정책과 S3 CORS 설정에서 처리합니다.

## 서버 배포 전제

운영 서버에는 CloudFront 기본 URL 또는 커스텀 CDN 도메인을 설정해야 합니다.

```env
AWS_S3_CDN_BASE_URL=https://cdn.example.com
```

마지막 `/`는 있어도 서버에서 정규화하지만, 없는 형태를 권장합니다.

이 값이 비어 있으면 서버는 S3 URL을 반환합니다. S3 버킷이 비공개인 운영 환경에서는 해당 URL이 `403 Forbidden`을 반환하므로 CDN 설정 없이 배포하면 관리 화면 미디어 프리뷰가 동작하지 않습니다.

