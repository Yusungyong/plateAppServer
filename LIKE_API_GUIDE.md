# 좋아요(Like) API 가이드

## 개요
동영상(Store) 및 이미지 피드에 대한 좋아요 기능 API입니다.
소프트 삭제 방식을 사용하여 좋아요 상태를 관리합니다.

---

## 1. 동영상(Store) 좋아요 API

### 1.1 좋아요 토글
사용자가 동영상에 좋아요를 누르거나 취소합니다.

**Endpoint:** `POST /api/stores/{storeId}/likes/toggle`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `storeId` (Integer, required): 동영상 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "isLiked": true,
    "likeCount": 42
  },
  "error": null
}
```

**Response Fields:**
- `isLiked` (boolean): 현재 좋아요 상태 (true=좋아요 활성, false=좋아요 취소)
- `likeCount` (long): 해당 동영상의 총 좋아요 수 (활성 상태만 카운트)

**동작 방식:**
- 좋아요가 없으면 → 새로 생성 (isLiked: true)
- 좋아요가 활성(Y)이면 → 비활성(N)으로 변경 (isLiked: false)
- 좋아요가 비활성(N)이면 → 활성(Y)으로 재활성화 (isLiked: true)

**Example:**
```javascript
// 좋아요 토글
const response = await fetch('/api/stores/337/likes/toggle', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const result = await response.json();
console.log(result.data.isLiked);    // true or false
console.log(result.data.likeCount);  // 42
```

---

### 1.2 좋아요 상태 조회
특정 동영상에 대한 사용자의 좋아요 상태와 총 좋아요 수를 조회합니다.

**Endpoint:** `GET /api/stores/{storeId}/likes/status`

**Headers:**
```
Authorization: Bearer {access_token}
```

**Path Parameters:**
- `storeId` (Integer, required): 동영상 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "isLiked": true,
    "likeCount": 42
  },
  "error": null
}
```

**Response Fields:**
- `isLiked` (boolean): 현재 사용자의 좋아요 상태
- `likeCount` (long): 해당 동영상의 총 좋아요 수

**Example:**
```javascript
// 좋아요 상태 확인
const response = await fetch('/api/stores/337/likes/status', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const result = await response.json();
if (result.data.isLiked) {
  // 하트 아이콘을 채워진 상태로 표시
}
```

---

### 1.3 좋아요 누른 사용자 목록 조회
특정 동영상에 좋아요를 누른 사용자 목록을 조회합니다.

**Endpoint:** `GET /api/stores/{storeId}/likes/users`

**Headers:**
```
Authorization: Bearer {access_token}
```

**Path Parameters:**
- `storeId` (Integer, required): 동영상 ID

**Query Parameters:**
- `limit` (int, optional, default: 20): 한 번에 가져올 사용자 수 (최대 100)
- `offset` (int, optional, default: 0): 시작 위치

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "userId": 12345,
        "username": "su12ng",
        "nickname": "수니",
        "profileImageUrl": "https://cdn.example.com/profile/su12ng.jpg",
        "activeRegion": "서울특별시 강남구",
        "likedAt": "2026-01-17T20:10:11"
      },
      {
        "userId": 67890,
        "username": "foodlover",
        "nickname": "맛집러버",
        "profileImageUrl": "https://cdn.example.com/profile/foodlover.jpg",
        "activeRegion": "서울특별시 마포구",
        "likedAt": "2026-01-17T19:45:30"
      }
    ],
    "totalCount": 42,
    "limit": 20,
    "offset": 0
  },
  "error": null
}
```

**Response Fields:**
- `items` (array): 사용자 목록
  - `userId` (int): 사용자 ID
  - `username` (string): 사용자명
  - `nickname` (string): 닉네임
  - `profileImageUrl` (string): 프로필 이미지 URL
  - `activeRegion` (string): 활동 지역
  - `likedAt` (datetime): 좋아요 누른 시각
- `totalCount` (long): 전체 좋아요 사용자 수
- `limit` (int): 요청한 limit 값
- `offset` (int): 요청한 offset 값

**Example:**
```javascript
// 좋아요 누른 사용자 목록 (페이지네이션)
const response = await fetch('/api/stores/337/likes/users?limit=20&offset=0', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const result = await response.json();
result.data.items.forEach(user => {
  console.log(`${user.nickname}님이 좋아요를 눌렀습니다.`);
});
```

---

## 2. 이미지 피드 좋아요 API

### 2.1 좋아요 토글
이미지 피드에 좋아요를 누르거나 취소합니다.

**Endpoint:** `POST /api/image-feeds/{feedId}/likes/toggle`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `feedId` (Integer, required): 이미지 피드 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "liked": true,
    "likeCount": 128
  },
  "error": null
}
```

**Response Fields:**
- `liked` (boolean): 현재 좋아요 상태
- `likeCount` (long): 해당 피드의 총 좋아요 수

**Example:**
```javascript
// 이미지 피드 좋아요 토글
const response = await fetch('/api/image-feeds/456/likes/toggle', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const result = await response.json();
console.log(result.data.liked);      // true or false
console.log(result.data.likeCount);  // 128
```

---

### 2.2 좋아요 수 조회
이미지 피드의 총 좋아요 수를 조회합니다.

**Endpoint:** `GET /api/image-feeds/{feedId}/likes/count`

**Path Parameters:**
- `feedId` (Integer, required): 이미지 피드 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": 128,
  "error": null
}
```

**Example:**
```javascript
// 좋아요 수만 조회
const response = await fetch('/api/image-feeds/456/likes/count');
const result = await response.json();
console.log(`좋아요 ${result.data}개`);
```

---

### 2.3 좋아요 누른 사용자 목록 조회
이미지 피드에 좋아요를 누른 사용자 목록을 조회합니다.

**Endpoint:** `GET /api/image-feeds/{feedId}/likes/users`

**Path Parameters:**
- `feedId` (Integer, required): 이미지 피드 ID

**Query Parameters:**
- `limit` (int, optional, default: 20): 한 번에 가져올 사용자 수 (최대 100)
- `offset` (int, optional, default: 0): 시작 위치

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "userId": 12345,
      "username": "su12ng",
      "nickname": "수니",
      "profileImageUrl": "https://cdn.example.com/profile/su12ng.jpg",
      "activeRegion": "서울특별시 강남구",
      "likedAt": "2026-01-17T20:10:11"
    }
  ],
  "error": null
}
```

**Example:**
```javascript
// 이미지 피드 좋아요 사용자 목록
const response = await fetch('/api/image-feeds/456/likes/users?limit=20&offset=0');
const result = await response.json();
```

---

## 3. 에러 처리

### 3.1 인증 오류
**Status:** `401 Unauthorized`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

### 3.2 리소스를 찾을 수 없음
**Status:** `404 Not Found`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_NOT_FOUND",
    "message": "요청한 리소스를 찾을 수 없습니다."
  }
}
```

### 3.3 잘못된 요청
**Status:** `400 Bad Request`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_INPUT",
    "message": "잘못된 요청입니다."
  }
}
```

---

## 4. 주요 변경사항

### 소프트 삭제 방식 적용
- 좋아요를 취소해도 DB에서 물리적으로 삭제되지 않습니다
- `use_yn` 필드: 'Y'(활성) / 'N'(비활성)
- `deleted_at` 필드: 좋아요 취소 시각 기록
- 좋아요 수 집계 시 `use_yn='Y'`인 레코드만 카운트

### 재활성화 지원
- 이전에 좋아요를 취소했다가 다시 누르면 기존 레코드를 재활성화
- 좋아요 히스토리가 유지됨

---

## 5. UI 구현 예시

### React 컴포넌트 예시
```javascript
import { useState, useEffect } from 'react';

function LikeButton({ storeId, accessToken }) {
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // 초기 좋아요 상태 로드
  useEffect(() => {
    fetchLikeStatus();
  }, [storeId]);

  const fetchLikeStatus = async () => {
    try {
      const response = await fetch(`/api/stores/${storeId}/likes/status`, {
        headers: {
          'Authorization': `Bearer ${accessToken}`
        }
      });
      const result = await response.json();
      if (result.success) {
        setIsLiked(result.data.isLiked);
        setLikeCount(result.data.likeCount);
      }
    } catch (error) {
      console.error('좋아요 상태 조회 실패:', error);
    }
  };

  const handleToggleLike = async () => {
    if (loading) return;

    setLoading(true);
    try {
      const response = await fetch(`/api/stores/${storeId}/likes/toggle`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      if (result.success) {
        setIsLiked(result.data.isLiked);
        setLikeCount(result.data.likeCount);
      }
    } catch (error) {
      console.error('좋아요 토글 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleToggleLike}
      disabled={loading}
      className={isLiked ? 'liked' : 'not-liked'}
    >
      {isLiked ? '❤️' : '🤍'} {likeCount}
    </button>
  );
}
```

---

## 6. 테스트 시나리오

### 시나리오 1: 좋아요 누르기
1. `GET /api/stores/337/likes/status` → `isLiked: false, likeCount: 10`
2. `POST /api/stores/337/likes/toggle` → `isLiked: true, likeCount: 11`
3. `GET /api/stores/337/likes/status` → `isLiked: true, likeCount: 11`

### 시나리오 2: 좋아요 취소
1. `GET /api/stores/337/likes/status` → `isLiked: true, likeCount: 11`
2. `POST /api/stores/337/likes/toggle` → `isLiked: false, likeCount: 10`
3. `GET /api/stores/337/likes/status` → `isLiked: false, likeCount: 10`

### 시나리오 3: 좋아요 재활성화
1. 좋아요 취소 상태에서
2. `POST /api/stores/337/likes/toggle` → `isLiked: true, likeCount: 11`
3. 이전 좋아요 기록이 재활성화됨

---

## 7. 참고사항

### 데이터베이스 스키마
- **fp_50**: 동영상(Store) 좋아요 테이블
  - PK: (username, store_id)
  - 컬럼: use_yn, deleted_at, created_at, updated_at

- **fp_60**: 이미지 피드 좋아요 테이블
  - PK: (feed_id, username)
  - 컬럼: use_yn, deleted_at, created_at, updated_at

### 인증
- 모든 좋아요 토글 API는 Bearer Token 인증이 필요합니다
- 좋아요 조회 API는 인증이 선택적일 수 있습니다 (구현에 따라 다름)

### 성능 최적화
- 좋아요 수는 실시간으로 집계되므로 캐싱 고려
- 사용자 목록 조회 시 페이지네이션 필수
- `use_yn` 인덱스가 적용되어 있어 활성 좋아요 조회가 빠름
