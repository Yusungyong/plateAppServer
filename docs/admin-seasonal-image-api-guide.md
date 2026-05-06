# Seasonal Source Image API Guide

Last updated: 2026-05-05

## Purpose

프런트 관리자 페이지에서 `fp_340` 제철 소스 음식 목록을 조회하고,
선택한 음식에 대표 이미지와 모바일 이미지를 등록/수정/삭제할 때 사용하는 API 문서입니다.

이번 정리 기준으로 `fp_910 ~ fp_914` 기반 운영 카드 API는 제거되었습니다.
현재 제철 이미지 저장 대상은 `fp_340`만 사용합니다.

## Base

- API base: `http://localhost:8090`
- Admin auth: `Authorization: Bearer {adminAccessToken}`

## DB target

이미지 메타데이터 저장 컬럼:
- `fp_340.card_image_url`
- `fp_340.card_image_mobile_url`

실제 파일 저장 위치:
- NAS 로컬 파일 경로
- 응답에는 브라우저에서 바로 사용할 수 있는 `/files/...` URL 반환

## 1. Source list 조회

`GET /api/admin/seasonal-foods/source-options`

Query:
- `month` optional

동작 규칙:
- `month`가 있으면 `fp_340.month = {month}` 기준 조회
- `month`가 없으면 서버가 `fp_341`에서 오늘 날짜에 해당하는 `seasonal_term`을 찾고, 그 term 기준으로 `fp_340` 조회
- 현재 운영 방식에서는 프런트에서 월을 직접 붙여 호출하는 것을 권장

예시:
```http
GET /api/admin/seasonal-foods/source-options?month=5
Authorization: Bearer {adminAccessToken}
```

응답 예시:
```json
{
  "items": [
    {
      "sourceFoodId": 78,
      "month": 5,
      "seasonalTerm": "소만",
      "category": "채소/나물",
      "foodName": "감자",
      "cardImageUrl": "/files/seasonal/desktop/2026/05/05/abc123-card.jpg",
      "cardImageMobileUrl": "/files/seasonal/mobile/2026/05/05/def456-card-mobile.jpg",
      "startDate": null,
      "endDate": null
    }
  ]
}
```

## 2. 이미지 등록

`POST /api/admin/seasonal-foods/source-options/{sourceFoodId}/images`

Content-Type:
- `multipart/form-data`

Fields:
- `imageType` required
- `file` required

`imageType`:
- `CARD_DESKTOP`
- `CARD_MOBILE`

예시:
```bash
curl -X POST "http://localhost:8090/api/admin/seasonal-foods/source-options/78/images" \
  -H "Authorization: Bearer {adminAccessToken}" \
  -F "imageType=CARD_DESKTOP" \
  -F "file=@./potato-card.jpg"
```

응답 예시:
```json
{
  "sourceFoodId": 78,
  "imageType": "CARD_DESKTOP",
  "imageUrl": "/files/seasonal/desktop/2026/05/05/abc123-card.jpg",
  "cardImageUrl": "/files/seasonal/desktop/2026/05/05/abc123-card.jpg",
  "cardImageMobileUrl": "/files/seasonal/mobile/2026/05/05/def456-card-mobile.jpg"
}
```

주의:
- 이미 값이 있는 타입에 `POST`를 호출하면 `409` 충돌
- 기존 이미지가 있으면 `PATCH` 사용

## 3. 이미지 수정

`PATCH /api/admin/seasonal-foods/source-options/{sourceFoodId}/images`

Fields:
- `imageType` required
- `file` required

예시:
```bash
curl -X PATCH "http://localhost:8090/api/admin/seasonal-foods/source-options/78/images" \
  -H "Authorization: Bearer {adminAccessToken}" \
  -F "imageType=CARD_MOBILE" \
  -F "file=@./potato-card-mobile.jpg"
```

## 4. 이미지 삭제

`DELETE /api/admin/seasonal-foods/source-options/{sourceFoodId}/images?imageType={type}`

예시:
```bash
curl -X DELETE "http://localhost:8090/api/admin/seasonal-foods/source-options/78/images?imageType=CARD_DESKTOP" \
  -H "Authorization: Bearer {adminAccessToken}"
```

응답 예시:
```json
{
  "sourceFoodId": 78,
  "imageType": "CARD_DESKTOP",
  "imageUrl": null,
  "cardImageUrl": null,
  "cardImageMobileUrl": "/files/seasonal/mobile/2026/05/05/def456-card-mobile.jpg"
}
```

## 5. 프런트 권장 흐름

1. 화면 진입 시 현재 월 계산
2. `GET /api/admin/seasonal-foods/source-options?month={현재월}` 호출
3. 목록에서 `sourceFoodId` 선택
4. 이미지가 없으면 `POST`
5. 이미지 교체면 `PATCH`
6. 이미지 삭제면 `DELETE`
7. 응답의 `cardImageUrl`, `cardImageMobileUrl`를 그대로 미리보기 `img src`에 사용

## 6. FormData 예시

```js
async function uploadSeasonalSourceImage(sourceFoodId, imageType, file, token, method = 'POST') {
  const formData = new FormData();
  formData.append('imageType', imageType);
  formData.append('file', file);

  const res = await fetch(`/api/admin/seasonal-foods/source-options/${sourceFoodId}/images`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: formData
  });

  if (!res.ok) {
    throw await res.json();
  }

  return res.json();
}
```

```js
async function deleteSeasonalSourceImage(sourceFoodId, imageType, token) {
  const res = await fetch(`/api/admin/seasonal-foods/source-options/${sourceFoodId}/images?imageType=${imageType}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  if (!res.ok) {
    throw await res.json();
  }

  return res.json();
}
```
