# PlateApp Backend API 요청서

## 개요
PlateApp 프론트엔드에서 필요한 서버 API 명세서입니다. 이 문서는 다음 기능들의 백엔드 구현을 위한 상세 스펙을 포함합니다:
- 좋아요(Likes) 시스템
- 댓글/답글(Comments/Replies) 시스템
- 알림(Notifications) 시스템
- 친구 관리(Friends) 시스템
- 프로필 편집(Profile) 시스템

---

## 📋 목차
1. [인증 및 공통 사항](#인증-및-공통-사항)
2. [좋아요 시스템](#좋아요-시스템)
3. [댓글/답글 시스템](#댓글답글-시스템)
4. [알림 시스템](#알림-시스템)
5. [친구 관리 시스템](#친구-관리-시스템)
6. [프로필 시스템](#프로필-시스템)
7. [데이터베이스 스키마](#데이터베이스-스키마)
8. [에러 코드](#에러-코드)

---

## 인증 및 공통 사항

### 인증 방식
- Bearer Token 방식 사용
- 모든 요청 헤더에 `Authorization: Bearer {token}` 포함

### 공통 응답 포맷
성공 응답:
```json
{
  "success": true,
  "data": {...}
}
```

에러 응답:
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly error message"
  }
}
```

### 페이지네이션
모든 리스트 API는 다음 쿼리 파라미터를 지원:
- `limit` (default: 20, max: 100)
- `offset` (default: 0)

응답 포맷:
```json
{
  "data": [...],
  "pagination": {
    "total": 100,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```

---

## 좋아요 시스템

### 데이터베이스 테이블
- `fp_50`: 이미지 피드 좋아요 (image_feed_id, user_id, created_at)
- `fp_60`: 비디오 피드 좋아요 (store_id, user_id, created_at)

### 1. 이미지 피드 좋아요 토글

**Endpoint:** `POST /image-feeds/{feedId}/likes/toggle`

**설명:** 이미지 피드에 좋아요를 추가하거나 취소합니다. 이미 좋아요가 있으면 삭제, 없으면 추가합니다.

**Request:**
```
POST /image-feeds/123/likes/toggle
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "isLiked": true,
  "likeCount": 42
}
```

**비즈니스 로직:**
- 현재 사용자가 해당 피드에 좋아요를 했는지 확인
- 좋아요가 있으면 삭제, 없으면 추가
- 전체 좋아요 수를 반환

---

### 2. 이미지 피드 좋아요한 사용자 목록

**Endpoint:** `GET /image-feeds/{feedId}/likes/users`

**설명:** 특정 이미지 피드를 좋아요한 사용자 목록을 가져옵니다.

**Request:**
```
GET /image-feeds/123/likes/users?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "userId": 456,
      "username": "john_doe",
      "nickname": "John",
      "profileImageUrl": "https://example.com/profile.jpg",
      "activeRegion": "서울시 강남구",
      "likedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "total": 42,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```

---

### 3. 이미지 피드 좋아요 상태 조회

**Endpoint:** `GET /image-feeds/{feedId}/likes/status`

**설명:** 현재 사용자가 특정 이미지 피드를 좋아요했는지, 전체 좋아요 수를 조회합니다.

**Request:**
```
GET /image-feeds/123/likes/status
Authorization: Bearer {token}
```

**Response:**
```json
{
  "isLiked": true,
  "likeCount": 42
}
```

---

### 4. 비디오 피드(Store) 좋아요 토글

**Endpoint:** `POST /stores/{storeId}/likes/toggle`

**설명:** 비디오 피드(Store)에 좋아요를 추가하거나 취소합니다.

**Request:**
```
POST /stores/789/likes/toggle
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "isLiked": true,
  "likeCount": 128
}
```

---

### 5. 비디오 피드 좋아요한 사용자 목록

**Endpoint:** `GET /stores/{storeId}/likes/users`

**Request:**
```
GET /stores/789/likes/users?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:** (이미지 피드와 동일한 포맷)

---

### 6. 비디오 피드 좋아요 상태 조회

**Endpoint:** `GET /stores/{storeId}/likes/status`

**Request:**
```
GET /stores/789/likes/status
Authorization: Bearer {token}
```

**Response:**
```json
{
  "isLiked": false,
  "likeCount": 128
}
```

---

## 댓글/답글 시스템

### 데이터베이스 테이블
- `fp_440`: 이미지 피드 댓글
- `fp_450`: 이미지 피드 답글
- `fp_460`: 비디오 피드 댓글
- `fp_470`: 비디오 피드 답글

### 공통 타입 정의
```typescript
type Comment = {
  commentId: number;
  userId: number;
  username: string;
  nickname?: string | null;
  profileImageUrl?: string | null;
  content: string;
  createdAt: string;
  updatedAt?: string | null;
  replyCount: number;  // 답글 개수
  isOwner: boolean;     // 현재 사용자가 작성자인지 여부
};

type Reply = {
  replyId: number;
  commentId: number;
  userId: number;
  username: string;
  nickname?: string | null;
  profileImageUrl?: string | null;
  content: string;
  createdAt: string;
  updatedAt?: string | null;
  isOwner: boolean;
};
```

---

### 1. 이미지 피드 댓글 목록 조회

**Endpoint:** `GET /image-feeds/{feedId}/comments`

**Request:**
```
GET /image-feeds/123/comments?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "commentId": 1,
      "userId": 456,
      "username": "john_doe",
      "nickname": "John",
      "profileImageUrl": "https://example.com/profile.jpg",
      "content": "멋진 사진이네요!",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": null,
      "replyCount": 3,
      "isOwner": false
    }
  ],
  "pagination": {
    "total": 15,
    "limit": 20,
    "offset": 0,
    "hasMore": false
  }
}
```

**정렬:** `createdAt` DESC (최신순)

---

### 2. 이미지 피드 댓글 작성

**Endpoint:** `POST /image-feeds/{feedId}/comments`

**Request:**
```json
POST /image-feeds/123/comments
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "정말 멋진 사진이네요!"
}
```

**Response:**
```json
{
  "commentId": 1,
  "userId": 456,
  "username": "john_doe",
  "nickname": "John",
  "profileImageUrl": "https://example.com/profile.jpg",
  "content": "정말 멋진 사진이네요!",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": null,
  "replyCount": 0,
  "isOwner": true
}
```

**검증:**
- `content`는 필수이며 1자 이상 500자 이하

---

### 3. 이미지 피드 댓글 수정

**Endpoint:** `PUT /image-feeds/{feedId}/comments/{commentId}`

**Request:**
```json
PUT /image-feeds/123/comments/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "수정된 댓글 내용입니다."
}
```

**Response:** (댓글 객체와 동일, `updatedAt` 포함)

**권한 체크:**
- 본인이 작성한 댓글만 수정 가능

---

### 4. 이미지 피드 댓글 삭제

**Endpoint:** `DELETE /image-feeds/{feedId}/comments/{commentId}`

**Request:**
```
DELETE /image-feeds/123/comments/1
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**권한 체크:**
- 본인이 작성한 댓글만 삭제 가능

**비즈니스 로직:**
- 댓글 삭제 시 해당 댓글의 모든 답글도 함께 삭제 (CASCADE)

---

### 5. 이미지 피드 답글 목록 조회

**Endpoint:** `GET /image-feeds/{feedId}/comments/{commentId}/replies`

**Request:**
```
GET /image-feeds/123/comments/1/replies?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "replyId": 1,
      "commentId": 1,
      "userId": 789,
      "username": "jane_smith",
      "nickname": "Jane",
      "profileImageUrl": "https://example.com/jane.jpg",
      "content": "저도 동감해요!",
      "createdAt": "2024-01-15T10:35:00Z",
      "updatedAt": null,
      "isOwner": false
    }
  ],
  "pagination": {
    "total": 3,
    "limit": 20,
    "offset": 0,
    "hasMore": false
  }
}
```

---

### 6. 이미지 피드 답글 작성

**Endpoint:** `POST /image-feeds/{feedId}/comments/{commentId}/replies`

**Request:**
```json
POST /image-feeds/123/comments/1/replies
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "저도 동감해요!"
}
```

**Response:** (답글 객체)

**비즈니스 로직:**
- 답글 작성 시 댓글의 `replyCount` 증가
- 댓글 작성자에게 알림 발송

---

### 7. 이미지 피드 답글 수정

**Endpoint:** `PUT /image-feeds/{feedId}/comments/{commentId}/replies/{replyId}`

**Request:**
```json
PUT /image-feeds/123/comments/1/replies/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "수정된 답글 내용입니다."
}
```

**Response:** (답글 객체, `updatedAt` 포함)

---

### 8. 이미지 피드 답글 삭제

**Endpoint:** `DELETE /image-feeds/{feedId}/comments/{commentId}/replies/{replyId}`

**Request:**
```
DELETE /image-feeds/123/comments/1/replies/1
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**비즈니스 로직:**
- 답글 삭제 시 댓글의 `replyCount` 감소

---

### 9-16. 비디오 피드 댓글/답글 API

비디오 피드(Store)의 댓글/답글 API는 이미지 피드와 동일한 구조이며, URL만 다릅니다:

- `POST /stores/{storeId}/comments`
- `GET /stores/{storeId}/comments`
- `PUT /stores/{storeId}/comments/{commentId}`
- `DELETE /stores/{storeId}/comments/{commentId}`
- `POST /stores/{storeId}/comments/{commentId}/replies`
- `GET /stores/{storeId}/comments/{commentId}/replies`
- `PUT /stores/{storeId}/comments/{commentId}/replies/{replyId}`
- `DELETE /stores/{storeId}/comments/{commentId}/replies/{replyId}`

---

## 알림 시스템

### 데이터베이스 테이블
- `fp_20`: 알림 테이블

### 알림 타입
```typescript
enum NotificationType {
  LIKE = 'LIKE',           // 좋아요
  COMMENT = 'COMMENT',     // 댓글
  REPLY = 'REPLY',         // 답글
  FOLLOW = 'FOLLOW',       // 친구 요청 수락
  MENTION = 'MENTION',     // 멘션
  SYSTEM = 'SYSTEM'        // 시스템 공지
}
```

### 타입 정의
```typescript
type Notification = {
  notificationId: number;
  userId: number;
  type: NotificationType;
  title: string;
  message: string;
  targetId?: number | null;      // 관련 게시물/댓글 ID
  targetType?: string | null;    // 'IMAGE_FEED' | 'VIDEO_FEED' | 'COMMENT'
  isRead: boolean;
  createdAt: string;
  actorUserId?: number | null;   // 행동을 한 사용자 ID
  actorUsername?: string | null;
  actorProfileImageUrl?: string | null;
};
```

---

### 1. 알림 목록 조회

**Endpoint:** `GET /notifications`

**Request:**
```
GET /notifications?limit=20&offset=0&unreadOnly=false
Authorization: Bearer {token}
```

**Query Parameters:**
- `limit` (optional): 페이지 크기
- `offset` (optional): 오프셋
- `unreadOnly` (optional, default: false): true면 읽지 않은 알림만 조회

**Response:**
```json
{
  "data": [
    {
      "notificationId": 1,
      "userId": 123,
      "type": "LIKE",
      "title": "새로운 좋아요",
      "message": "john_doe님이 회원님의 게시물을 좋아합니다.",
      "targetId": 456,
      "targetType": "IMAGE_FEED",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00Z",
      "actorUserId": 789,
      "actorUsername": "john_doe",
      "actorProfileImageUrl": "https://example.com/profile.jpg"
    }
  ],
  "pagination": {
    "total": 25,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```

**정렬:** `createdAt` DESC (최신순)

---

### 2. 읽지 않은 알림 개수 조회

**Endpoint:** `GET /notifications/unread-count`

**Request:**
```
GET /notifications/unread-count
Authorization: Bearer {token}
```

**Response:**
```json
{
  "count": 5
}
```

---

### 3. 알림 읽음 처리

**Endpoint:** `PUT /notifications/{notificationId}/read`

**Request:**
```
PUT /notifications/123/read
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**비즈니스 로직:**
- `isRead`를 `true`로 업데이트

---

### 4. 모든 알림 읽음 처리

**Endpoint:** `PUT /notifications/read-all`

**Request:**
```
PUT /notifications/read-all
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "updatedCount": 5
}
```

---

### 5. 알림 삭제

**Endpoint:** `DELETE /notifications/{notificationId}`

**Request:**
```
DELETE /notifications/123
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

---

### 6. 모든 알림 삭제

**Endpoint:** `DELETE /notifications/all`

**Request:**
```
DELETE /notifications/all
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "deletedCount": 25
}
```

---

### 알림 생성 트리거

다음 이벤트 발생 시 자동으로 알림 생성:

1. **좋아요 (LIKE)**
   - 누군가 내 게시물에 좋아요를 할 때
   - `targetId`: 게시물 ID
   - `targetType`: 'IMAGE_FEED' | 'VIDEO_FEED'

2. **댓글 (COMMENT)**
   - 누군가 내 게시물에 댓글을 작성할 때
   - `targetId`: 댓글 ID
   - `targetType`: 'COMMENT'

3. **답글 (REPLY)**
   - 누군가 내 댓글에 답글을 작성할 때
   - `targetId`: 답글 ID
   - `targetType`: 'REPLY'

4. **친구 요청 수락 (FOLLOW)**
   - 누군가 내 친구 요청을 수락할 때
   - `targetId`: null
   - `targetType`: null

---

## 친구 관리 시스템

### 데이터베이스 테이블
- `fp_150`: 친구 관계 테이블
- `fp_160`: 친구 요청 테이블

### 친구 요청 상태
```typescript
enum FriendRequestStatus {
  PENDING = 'PENDING',     // 대기 중
  ACCEPTED = 'ACCEPTED',   // 수락됨
  REJECTED = 'REJECTED'    // 거절됨
}
```

### 타입 정의
```typescript
type Friend = {
  userId: number;
  username: string;
  nickname?: string | null;
  profileImageUrl?: string | null;
  activeRegion?: string | null;
  friendsSince?: string | null;  // 친구가 된 날짜
};

type FriendRequest = {
  requestId: number;
  fromUserId: number;
  fromUsername: string;
  fromNickname?: string | null;
  fromProfileImageUrl?: string | null;
  toUserId: number;
  toUsername: string;
  status: FriendRequestStatus;
  createdAt: string;
  respondedAt?: string | null;
};

type FriendSearchResult = {
  userId: number;
  username: string;
  nickname?: string | null;
  profileImageUrl?: string | null;
  activeRegion?: string | null;
  isFriend: boolean;      // 이미 친구인지
  isPending: boolean;     // 친구 요청 중인지
};
```

---

### 1. 친구 목록 조회

**Endpoint:** `GET /friends`

**Request:**
```
GET /friends?limit=50&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "userId": 456,
      "username": "john_doe",
      "nickname": "John",
      "profileImageUrl": "https://example.com/profile.jpg",
      "activeRegion": "서울시 강남구",
      "friendsSince": "2024-01-01T00:00:00Z"
    }
  ],
  "pagination": {
    "total": 45,
    "limit": 50,
    "offset": 0,
    "hasMore": false
  }
}
```

**정렬:** `friendsSince` DESC (최근에 친구가 된 순)

---

### 2. 사용자 검색

**Endpoint:** `GET /friends/search`

**Request:**
```
GET /friends/search?q=john&limit=10&offset=0
Authorization: Bearer {token}
```

**Query Parameters:**
- `q` (required): 검색 키워드 (username 또는 nickname 검색)
- `limit` (optional, default: 10)
- `offset` (optional, default: 0)

**Response:**
```json
{
  "data": [
    {
      "userId": 456,
      "username": "john_doe",
      "nickname": "John",
      "profileImageUrl": "https://example.com/profile.jpg",
      "activeRegion": "서울시 강남구",
      "isFriend": false,
      "isPending": true
    }
  ],
  "pagination": {
    "total": 5,
    "limit": 10,
    "offset": 0,
    "hasMore": false
  }
}
```

---

### 3. 친구 삭제

**Endpoint:** `DELETE /friends/{userId}`

**Request:**
```
DELETE /friends/456
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**비즈니스 로직:**
- 양방향 친구 관계 모두 삭제
- `fp_150` 테이블에서 (user_id=current, friend_id=456) 및 (user_id=456, friend_id=current) 삭제

---

### 4. 보낸 친구 요청 목록 조회

**Endpoint:** `GET /friends/requests/sent`

**Request:**
```
GET /friends/requests/sent?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "requestId": 1,
      "fromUserId": 123,
      "fromUsername": "me",
      "fromNickname": "나",
      "fromProfileImageUrl": "https://example.com/me.jpg",
      "toUserId": 456,
      "toUsername": "john_doe",
      "status": "PENDING",
      "createdAt": "2024-01-15T10:00:00Z",
      "respondedAt": null
    }
  ],
  "pagination": {
    "total": 3,
    "limit": 20,
    "offset": 0,
    "hasMore": false
  }
}
```

---

### 5. 받은 친구 요청 목록 조회

**Endpoint:** `GET /friends/requests/received`

**Request:**
```
GET /friends/requests/received?limit=20&offset=0
Authorization: Bearer {token}
```

**Response:**
```json
{
  "data": [
    {
      "requestId": 2,
      "fromUserId": 789,
      "fromUsername": "jane_smith",
      "fromNickname": "Jane",
      "fromProfileImageUrl": "https://example.com/jane.jpg",
      "toUserId": 123,
      "toUsername": "me",
      "status": "PENDING",
      "createdAt": "2024-01-15T11:00:00Z",
      "respondedAt": null
    }
  ],
  "pagination": {
    "total": 2,
    "limit": 20,
    "offset": 0,
    "hasMore": false
  }
}
```

---

### 6. 친구 요청 보내기

**Endpoint:** `POST /friends/requests`

**Request:**
```json
POST /friends/requests
Authorization: Bearer {token}
Content-Type: application/json

{
  "toUserId": 456
}
```

**Response:**
```json
{
  "requestId": 1,
  "fromUserId": 123,
  "fromUsername": "me",
  "fromNickname": "나",
  "fromProfileImageUrl": "https://example.com/me.jpg",
  "toUserId": 456,
  "toUsername": "john_doe",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:00:00Z",
  "respondedAt": null
}
```

**검증:**
- 자기 자신에게 요청 불가
- 이미 친구인 경우 요청 불가
- 이미 대기 중인 요청이 있는 경우 중복 요청 불가

**비즈니스 로직:**
- 친구 요청 생성
- 받는 사람에게 알림 발송

---

### 7. 친구 요청 취소

**Endpoint:** `DELETE /friends/requests/{requestId}`

**Request:**
```
DELETE /friends/requests/1
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**권한 체크:**
- 본인이 보낸 요청만 취소 가능 (`fromUserId`가 현재 사용자)

---

### 8. 친구 요청 수락

**Endpoint:** `PUT /friends/requests/{requestId}/accept`

**Request:**
```
PUT /friends/requests/2/accept
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**권한 체크:**
- 본인에게 온 요청만 수락 가능 (`toUserId`가 현재 사용자)

**비즈니스 로직:**
1. 요청 상태를 `ACCEPTED`로 변경
2. `respondedAt`을 현재 시간으로 설정
3. `fp_150` 테이블에 양방향 친구 관계 추가
4. 요청 보낸 사람에게 알림 발송

---

### 9. 친구 요청 거절

**Endpoint:** `PUT /friends/requests/{requestId}/reject`

**Request:**
```
PUT /friends/requests/2/reject
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**권한 체크:**
- 본인에게 온 요청만 거절 가능 (`toUserId`가 현재 사용자)

**비즈니스 로직:**
1. 요청 상태를 `REJECTED`로 변경
2. `respondedAt`을 현재 시간으로 설정
3. (선택) 거절된 요청은 일정 시간 후 자동 삭제

---

## 프로필 시스템

### 데이터베이스 테이블
- `fp_100`: 사용자 기본 정보
- `fp_101`: 사용자 상세 정보

### 타입 정의
```typescript
type UserProfile = {
  userId: number;
  username: string;
  nickname?: string | null;
  profileImageUrl?: string | null;
  bio?: string | null;
  activeRegion?: string | null;
  email?: string | null;
  phoneNumber?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

type UserStats = {
  friendsCount: number;
  postsCount: number;
  likesCount: number;
  visitedStoresCount: number;
};
```

---

### 1. 내 프로필 조회

**Endpoint:** `GET /users/me`

**Request:**
```
GET /users/me
Authorization: Bearer {token}
```

**Response:**
```json
{
  "userId": 123,
  "username": "john_doe",
  "nickname": "John",
  "profileImageUrl": "https://example.com/profile.jpg",
  "bio": "안녕하세요! 맛집 탐방을 좋아합니다.",
  "activeRegion": "서울시 강남구",
  "email": "john@example.com",
  "phoneNumber": "010-1234-5678",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z"
}
```

---

### 2. 다른 사용자 프로필 조회

**Endpoint:** `GET /users/{username}`

**Request:**
```
GET /users/jane_smith
Authorization: Bearer {token}
```

**Response:** (내 프로필과 동일한 포맷, 단 `email`과 `phoneNumber`는 제외될 수 있음)

---

### 3. 프로필 수정

**Endpoint:** `PUT /users/me`

**Request:**
```json
PUT /users/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "nickname": "새로운 닉네임",
  "bio": "수정된 소개글입니다.",
  "activeRegion": "서울시 서초구",
  "email": "newemail@example.com",
  "phoneNumber": "010-9876-5432"
}
```

**Response:**
```json
{
  "userId": 123,
  "username": "john_doe",
  "nickname": "새로운 닉네임",
  "profileImageUrl": "https://example.com/profile.jpg",
  "bio": "수정된 소개글입니다.",
  "activeRegion": "서울시 서초구",
  "email": "newemail@example.com",
  "phoneNumber": "010-9876-5432",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-16T09:00:00Z"
}
```

**검증:**
- `nickname`: 1-20자
- `bio`: 최대 200자
- `email`: 유효한 이메일 형식

---

### 4. 프로필 이미지 업로드

**Endpoint:** `POST /users/me/profile-image`

**Request:**
```
POST /users/me/profile-image
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [binary image data]
```

**Response:**
```json
{
  "profileImageUrl": "https://example.com/profile-new.jpg"
}
```

**검증:**
- 파일 크기: 최대 5MB
- 파일 형식: jpg, jpeg, png, gif
- 이미지 최대 해상도: 2048x2048

**비즈니스 로직:**
1. 이미지를 CDN/S3에 업로드
2. 이전 프로필 이미지 삭제 (선택)
3. `fp_100` 테이블의 `profile_image_url` 업데이트

---

### 5. 프로필 이미지 삭제

**Endpoint:** `DELETE /users/me/profile-image`

**Request:**
```
DELETE /users/me/profile-image
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true
}
```

**비즈니스 로직:**
- CDN/S3에서 이미지 파일 삭제
- `profile_image_url`을 `null`로 설정

---

### 6. 비밀번호 변경

**Endpoint:** `PUT /users/me/password`

**Request:**
```json
PUT /users/me/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword456"
}
```

**Response:**
```json
{
  "success": true
}
```

**검증:**
- `currentPassword`: 현재 비밀번호 확인
- `newPassword`: 8자 이상, 영문/숫자/특수문자 조합

---

### 7. 사용자 통계 조회

**Endpoint:** `GET /users/me/stats` 또는 `GET /users/{username}/stats`

**Request:**
```
GET /users/me/stats
Authorization: Bearer {token}
```

**Response:**
```json
{
  "friendsCount": 45,
  "postsCount": 128,
  "likesCount": 456,
  "visitedStoresCount": 89
}
```

**설명:**
- `friendsCount`: 친구 수
- `postsCount`: 작성한 게시물 수 (이미지 + 비디오)
- `likesCount`: 받은 좋아요 총합
- `visitedStoresCount`: 방문한 가게 수

---

### 8. 계정 삭제

**Endpoint:** `DELETE /users/me`

**Request:**
```json
DELETE /users/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "password": "mypassword123"
}
```

**Response:**
```json
{
  "success": true
}
```

**비즈니스 로직:**
1. 비밀번호 확인
2. 사용자가 작성한 모든 게시물, 댓글, 답글 삭제
3. 친구 관계 모두 삭제
4. 프로필 이미지 삭제
5. 사용자 계정 삭제 또는 비활성화

---

## 데이터베이스 스키마

### fp_50: 이미지 피드 좋아요
```sql
CREATE TABLE fp_50 (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  image_feed_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feed_user (image_feed_id, user_id),
  INDEX idx_feed (image_feed_id),
  INDEX idx_user (user_id)
);
```

### fp_60: 비디오 피드(Store) 좋아요
```sql
CREATE TABLE fp_60 (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_store_user (store_id, user_id),
  INDEX idx_store (store_id),
  INDEX idx_user (user_id)
);
```

### fp_440: 이미지 피드 댓글
```sql
CREATE TABLE fp_440 (
  comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  image_feed_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL,
  INDEX idx_feed (image_feed_id),
  INDEX idx_user (user_id)
);
```

### fp_450: 이미지 피드 답글
```sql
CREATE TABLE fp_450 (
  reply_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL,
  INDEX idx_comment (comment_id),
  INDEX idx_user (user_id)
);
```

### fp_460: 비디오 피드 댓글
```sql
CREATE TABLE fp_460 (
  comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL,
  INDEX idx_store (store_id),
  INDEX idx_user (user_id)
);
```

### fp_470: 비디오 피드 답글
```sql
CREATE TABLE fp_470 (
  reply_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL,
  INDEX idx_comment (comment_id),
  INDEX idx_user (user_id)
);
```

### fp_20: 알림
```sql
CREATE TABLE fp_20 (
  notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  message VARCHAR(255) NOT NULL,
  target_id BIGINT NULL,
  target_type VARCHAR(50) NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  actor_user_id BIGINT NULL,
  INDEX idx_user (user_id),
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_created (created_at)
);
```

### fp_150: 친구 관계
```sql
CREATE TABLE fp_150 (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  friend_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_friend (user_id, friend_id),
  INDEX idx_user (user_id),
  INDEX idx_friend (friend_id)
);
```

### fp_160: 친구 요청
```sql
CREATE TABLE fp_160 (
  request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  responded_at TIMESTAMP NULL,
  INDEX idx_from (from_user_id),
  INDEX idx_to (to_user_id),
  INDEX idx_status (status)
);
```

### fp_100: 사용자 기본 정보
```sql
CREATE TABLE fp_100 (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(20) NULL,
  profile_image_url VARCHAR(500) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL,
  INDEX idx_username (username)
);
```

### fp_101: 사용자 상세 정보
```sql
CREATE TABLE fp_101 (
  user_id BIGINT PRIMARY KEY,
  bio VARCHAR(200) NULL,
  active_region VARCHAR(100) NULL,
  email VARCHAR(100) NULL,
  phone_number VARCHAR(20) NULL,
  FOREIGN KEY (user_id) REFERENCES fp_100(user_id) ON DELETE CASCADE
);
```

---

## 에러 코드

### 인증/권한 (4xx)
- `AUTH_REQUIRED` (401): 인증이 필요합니다
- `INVALID_TOKEN` (401): 유효하지 않은 토큰입니다
- `TOKEN_EXPIRED` (401): 토큰이 만료되었습니다
- `FORBIDDEN` (403): 권한이 없습니다

### 유효성 검증 (4xx)
- `INVALID_INPUT` (400): 잘못된 입력입니다
- `MISSING_REQUIRED_FIELD` (400): 필수 필드가 누락되었습니다
- `INVALID_EMAIL` (400): 유효하지 않은 이메일 형식입니다
- `PASSWORD_TOO_SHORT` (400): 비밀번호가 너무 짧습니다
- `CONTENT_TOO_LONG` (400): 내용이 너무 깁니다

### 리소스 (4xx)
- `NOT_FOUND` (404): 리소스를 찾을 수 없습니다
- `ALREADY_EXISTS` (409): 이미 존재합니다
- `DUPLICATE_REQUEST` (409): 중복된 요청입니다

### 비즈니스 로직 (4xx)
- `ALREADY_LIKED` (409): 이미 좋아요한 게시물입니다
- `NOT_LIKED` (409): 좋아요하지 않은 게시물입니다
- `ALREADY_FRIENDS` (409): 이미 친구입니다
- `FRIEND_REQUEST_PENDING` (409): 이미 친구 요청이 대기 중입니다
- `CANNOT_FRIEND_SELF` (400): 자기 자신에게 친구 요청할 수 없습니다
- `NOT_OWNER` (403): 작성자만 수정/삭제할 수 있습니다

### 서버 (5xx)
- `INTERNAL_SERVER_ERROR` (500): 서버 내부 오류가 발생했습니다
- `SERVICE_UNAVAILABLE` (503): 서비스를 일시적으로 사용할 수 없습니다

---

## 구현 우선순위

### Phase 1 (필수 기능)
1. 좋아요 시스템
   - 토글 API
   - 좋아요 상태 조회
   - 좋아요한 사용자 목록

2. 댓글/답글 시스템
   - 댓글 CRUD
   - 답글 CRUD

### Phase 2 (핵심 기능)
3. 알림 시스템
   - 알림 조회
   - 읽음 처리
   - 자동 알림 생성

4. 친구 관리
   - 친구 목록
   - 친구 요청/수락/거절

### Phase 3 (추가 기능)
5. 프로필 시스템
   - 프로필 조회/수정
   - 프로필 이미지 업로드
   - 사용자 통계

---

## 테스트 케이스

### 좋아요 시스템
- [ ] 좋아요 추가 성공
- [ ] 좋아요 취소 성공
- [ ] 동일한 게시물에 중복 좋아요 방지
- [ ] 존재하지 않는 게시물에 좋아요 시도 시 404
- [ ] 인증되지 않은 사용자의 좋아요 시도 시 401

### 댓글/답글 시스템
- [ ] 댓글 작성 성공
- [ ] 댓글 수정 성공 (본인만)
- [ ] 댓글 삭제 성공 (본인만)
- [ ] 답글 작성 시 댓글 replyCount 증가
- [ ] 답글 삭제 시 댓글 replyCount 감소
- [ ] 댓글 삭제 시 답글 CASCADE 삭제
- [ ] 타인의 댓글 수정/삭제 시도 시 403

### 알림 시스템
- [ ] 좋아요 발생 시 알림 자동 생성
- [ ] 댓글 작성 시 알림 자동 생성
- [ ] 알림 읽음 처리 성공
- [ ] 읽지 않은 알림 개수 조회 정확성

### 친구 관리 시스템
- [ ] 친구 요청 보내기 성공
- [ ] 중복 친구 요청 방지
- [ ] 이미 친구인 경우 요청 방지
- [ ] 친구 요청 수락 시 양방향 관계 생성
- [ ] 친구 삭제 시 양방향 관계 모두 삭제

### 프로필 시스템
- [ ] 프로필 수정 성공
- [ ] 프로필 이미지 업로드 성공
- [ ] 잘못된 이미지 형식 업로드 시 400
- [ ] 비밀번호 변경 성공 (현재 비밀번호 확인)

---

## 성능 고려사항

### 인덱싱
- 모든 외래 키에 인덱스 추가
- 자주 조회되는 컬럼에 인덱스 추가 (`user_id`, `created_at`)
- 복합 인덱스 고려 (`user_id, is_read` for 알림)

### 캐싱
- 좋아요 수, 댓글 수는 Redis에 캐싱 고려
- 프로필 정보는 5분간 캐싱
- 친구 목록은 변경 시까지 캐싱

### 페이지네이션
- 모든 리스트 API는 Offset 기반 페이지네이션 사용
- `limit` 최대값 제한 (100)

### N+1 쿼리 방지
- 좋아요한 사용자 목록 조회 시 JOIN 사용
- 댓글 목록 조회 시 작성자 정보 JOIN

---

## 보안 고려사항

### 인증/권한
- 모든 API는 JWT 토큰 검증 필수
- 리소스 소유자 확인 (댓글 수정/삭제)

### Rate Limiting
- 좋아요 토글: 10회/분
- 댓글 작성: 5회/분
- 친구 요청: 10회/시간

### SQL Injection 방지
- Prepared Statement 사용
- ORM 사용 권장

### XSS 방지
- 댓글/답글 내용은 HTML 태그 이스케이프

---

## 문의사항
API 구현 중 궁금한 사항이나 변경 요청이 있으면 프론트엔드 팀에 문의해주세요.
