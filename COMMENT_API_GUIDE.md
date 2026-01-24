# 댓글/답글 API 가이드

## 개요
동영상(Store)에 대한 댓글(Comment)과 답글(Reply) 기능 API입니다.
소프트 삭제 방식을 사용하여 댓글/답글 상태를 관리합니다.

---

## 목차
1. [댓글 API](#1-댓글-api)
2. [답글 API](#2-답글-api)
3. [에러 처리](#3-에러-처리)
4. [데이터 구조](#4-데이터-구조)
5. [UI 구현 예시](#5-ui-구현-예시)

---

## 1. 댓글 API

### 1.1 댓글 목록 조회
특정 동영상의 댓글 목록을 페이지네이션으로 조회합니다.

**Endpoint:** `GET /api/stores/{storeId}/comments`

**인증:** 불필요 (공개 API)

**Path Parameters:**
- `storeId` (Integer, required): 동영상 ID

**Query Parameters:**
- `page` (int, optional, default: 0): 페이지 번호 (0부터 시작)
- `size` (int, optional, default: 20): 페이지당 댓글 수 (최대 50)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "page": 0,
    "size": 20,
    "total": 156,
    "items": [
      {
        "commentId": 1234,
        "storeId": 337,
        "content": "정말 맛있어 보이네요! 다음에 꼭 가보고 싶어요.",
        "createdAt": "2026-01-17T20:10:11",
        "updatedAt": "2026-01-17T20:10:11",
        "author": {
          "username": "su12ng",
          "userId": 12345,
          "nickName": "수니",
          "profileImageUrl": "https://cdn.example.com/profile/su12ng.jpg",
          "isPrivate": false
        },
        "replyCount": 3,
        "replies": []
      }
    ]
  },
  "error": null
}
```

**Response Fields:**
- `page` (int): 현재 페이지 번호
- `size` (int): 페이지당 항목 수
- `total` (long): 전체 댓글 수
- `items` (array): 댓글 목록
  - `commentId` (int): 댓글 ID
  - `storeId` (int): 동영상 ID
  - `content` (string): 댓글 내용
  - `createdAt` (datetime): 생성 시각
  - `updatedAt` (datetime): 수정 시각
  - `author` (object): 작성자 정보
    - `username` (string): 사용자명
    - `userId` (int): 사용자 ID
    - `nickName` (string): 닉네임
    - `profileImageUrl` (string): 프로필 이미지 URL
    - `isPrivate` (boolean): 비공개 계정 여부
  - `replyCount` (long): 답글 개수
  - `replies` (array): 답글 목록 (댓글 목록 조회 시에는 비어있음, 답글은 별도 API로 조회)

**Example:**
```javascript
// 댓글 목록 조회 (첫 페이지)
const response = await fetch('/api/stores/337/comments?page=0&size=20');
const result = await response.json();

result.data.items.forEach(comment => {
  console.log(`${comment.author.nickName}: ${comment.content}`);
  console.log(`답글 ${comment.replyCount}개`);
});
```

**중요:**
- 댓글 목록 조회 시 `replies` 배열은 비어있습니다
- 답글은 `replyCount`로 개수만 확인하고, 답글 목록은 별도 API(`GET /api/comments/{commentId}/replies`)로 조회하세요
- 최신 댓글부터 내림차순으로 정렬됩니다

---

### 1.2 댓글 작성
동영상에 새 댓글을 작성합니다.

**Endpoint:** `POST /api/stores/{storeId}/comments`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `storeId` (Integer, required): 동영상 ID

**Request Body:**
```json
{
  "content": "정말 맛있어 보이네요!"
}
```

**Request Fields:**
- `content` (string, required): 댓글 내용 (1~2000자)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "commentId": 1234
  },
  "error": null
}
```

**Response Fields:**
- `commentId` (int): 생성된 댓글 ID

**Example:**
```javascript
// 댓글 작성
const response = await fetch('/api/stores/337/comments', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    content: '정말 맛있어 보이네요!'
  })
});

const result = await response.json();
console.log(`댓글 ID: ${result.data.commentId}`);
```

**Validation:**
- 내용이 비어있으면 `400 Bad Request`
- 내용이 2000자를 초과하면 `400 Bad Request`
- 인증되지 않은 사용자는 `401 Unauthorized`

---

### 1.3 댓글 수정
자신이 작성한 댓글을 수정합니다.

**Endpoint:** `PUT /api/comments/{commentId}`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `commentId` (Integer, required): 댓글 ID

**Request Body:**
```json
{
  "content": "수정된 댓글 내용입니다."
}
```

**Request Fields:**
- `content` (string, required): 수정할 댓글 내용 (1~2000자)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "ok": true
  },
  "error": null
}
```

**Example:**
```javascript
// 댓글 수정
const response = await fetch('/api/comments/1234', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    content: '수정된 댓글 내용입니다.'
  })
});
```

**권한:**
- 본인이 작성한 댓글만 수정 가능
- 다른 사용자의 댓글 수정 시도 시 `403 Forbidden` (SecurityException)

---

### 1.4 댓글 삭제
자신이 작성한 댓글을 삭제합니다. 소프트 삭제 방식으로 처리되며, 해당 댓글의 모든 답글도 함께 삭제됩니다.

**Endpoint:** `DELETE /api/comments/{commentId}`

**Headers:**
```
Authorization: Bearer {access_token}
```

**Path Parameters:**
- `commentId` (Integer, required): 댓글 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "ok": true
  },
  "error": null
}
```

**Example:**
```javascript
// 댓글 삭제
const response = await fetch('/api/comments/1234', {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

**중요:**
- 댓글 삭제 시 해당 댓글의 **모든 답글도 함께 소프트 삭제**됩니다
- 물리적으로 삭제되지 않고 `use_yn='N'`, `deleted_at` 설정됩니다
- 삭제된 댓글/답글은 목록 조회 시 노출되지 않습니다

**권한:**
- 본인이 작성한 댓글만 삭제 가능
- 다른 사용자의 댓글 삭제 시도 시 `403 Forbidden`

---

## 2. 답글 API

### 2.1 답글 목록 조회
특정 댓글의 답글 목록을 페이지네이션으로 조회합니다.

**Endpoint:** `GET /api/comments/{commentId}/replies`

**인증:** 불필요 (공개 API)

**Path Parameters:**
- `commentId` (Integer, required): 댓글 ID

**Query Parameters:**
- `page` (int, optional, default: 0): 페이지 번호 (0부터 시작)
- `size` (int, optional, default: 50): 페이지당 답글 수 (최대 50)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "page": 0,
    "size": 50,
    "total": 3,
    "items": [
      {
        "replyId": 5678,
        "commentId": 1234,
        "content": "저도 가봤는데 진짜 맛있어요!",
        "createdAt": "2026-01-17T20:15:30",
        "updatedAt": "2026-01-17T20:15:30",
        "author": {
          "username": "foodlover",
          "userId": 67890,
          "nickName": "맛집러버",
          "profileImageUrl": "https://cdn.example.com/profile/foodlover.jpg",
          "isPrivate": false
        }
      }
    ]
  },
  "error": null
}
```

**Response Fields:**
- `page` (int): 현재 페이지 번호
- `size` (int): 페이지당 항목 수
- `total` (long): 전체 답글 수
- `items` (array): 답글 목록
  - `replyId` (int): 답글 ID
  - `commentId` (int): 부모 댓글 ID
  - `content` (string): 답글 내용
  - `createdAt` (datetime): 생성 시각
  - `updatedAt` (datetime): 수정 시각
  - `author` (object): 작성자 정보

**Example:**
```javascript
// 답글 목록 조회
const response = await fetch('/api/comments/1234/replies?page=0&size=50');
const result = await response.json();

result.data.items.forEach(reply => {
  console.log(`  ↳ ${reply.author.nickName}: ${reply.content}`);
});
```

**중요:**
- 답글은 작성 시간 순으로 오름차순 정렬됩니다 (오래된 답글부터)
- 부모 댓글이 삭제되었거나 존재하지 않으면 `404 Not Found`

---

### 2.2 답글 작성
댓글에 답글을 작성합니다.

**Endpoint:** `POST /api/comments/{commentId}/replies`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `commentId` (Integer, required): 부모 댓글 ID

**Request Body:**
```json
{
  "content": "저도 가봤는데 진짜 맛있어요!"
}
```

**Request Fields:**
- `content` (string, required): 답글 내용 (1~2000자)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "replyId": 5678
  },
  "error": null
}
```

**Response Fields:**
- `replyId` (int): 생성된 답글 ID

**Example:**
```javascript
// 답글 작성
const response = await fetch('/api/comments/1234/replies', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    content: '저도 가봤는데 진짜 맛있어요!'
  })
});

const result = await response.json();
console.log(`답글 ID: ${result.data.replyId}`);
```

**Validation:**
- 내용이 비어있으면 `400 Bad Request`
- 내용이 2000자를 초과하면 `400 Bad Request`
- 부모 댓글이 삭제되었거나 존재하지 않으면 `404 Not Found`
- 인증되지 않은 사용자는 `401 Unauthorized`

---

### 2.3 답글 수정
자신이 작성한 답글을 수정합니다.

**Endpoint:** `PUT /api/replies/{replyId}`

**Headers:**
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Path Parameters:**
- `replyId` (Integer, required): 답글 ID

**Request Body:**
```json
{
  "content": "수정된 답글 내용입니다."
}
```

**Request Fields:**
- `content` (string, required): 수정할 답글 내용 (1~2000자)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "ok": true
  },
  "error": null
}
```

**Example:**
```javascript
// 답글 수정
const response = await fetch('/api/replies/5678', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    content: '수정된 답글 내용입니다.'
  })
});
```

**권한:**
- 본인이 작성한 답글만 수정 가능
- 다른 사용자의 답글 수정 시도 시 `403 Forbidden`

---

### 2.4 답글 삭제
자신이 작성한 답글을 삭제합니다. 소프트 삭제 방식으로 처리됩니다.

**Endpoint:** `DELETE /api/replies/{replyId}`

**Headers:**
```
Authorization: Bearer {access_token}
```

**Path Parameters:**
- `replyId` (Integer, required): 답글 ID

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "ok": true
  },
  "error": null
}
```

**Example:**
```javascript
// 답글 삭제
const response = await fetch('/api/replies/5678', {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

**중요:**
- 물리적으로 삭제되지 않고 `use_yn='N'`, `deleted_at` 설정됩니다
- 삭제된 답글은 목록 조회 시 노출되지 않습니다

**권한:**
- 본인이 작성한 답글만 삭제 가능
- 다른 사용자의 답글 삭제 시도 시 `403 Forbidden`

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

### 3.2 권한 오류
**Status:** `403 Forbidden`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_FORBIDDEN",
    "message": "not owner"
  }
}
```

### 3.3 리소스를 찾을 수 없음
**Status:** `404 Not Found`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_NOT_FOUND",
    "message": "comment not found"
  }
}
```

### 3.4 잘못된 요청
**Status:** `400 Bad Request`
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_INPUT",
    "message": "content is empty"
  }
}
```

**가능한 에러 메시지:**
- `"content is empty"`: 내용이 비어있음
- `"content too long"`: 내용이 2000자 초과

---

## 4. 데이터 구조

### 4.1 데이터베이스 스키마

#### fp_440 (동영상 댓글)
```sql
comment_id    INTEGER PRIMARY KEY (Auto Increment)
store_id      INTEGER NOT NULL (FK -> fp_300.store_id)
username      VARCHAR(255) NOT NULL (FK -> fp_100.username)
user_id       INTEGER (FK -> fp_100.user_id, nullable)
content       TEXT NOT NULL
created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
use_yn        VARCHAR(1) NOT NULL DEFAULT 'Y'
deleted_at    DATE (소프트 삭제 시각)
```

#### fp_450 (동영상 답글)
```sql
reply_id      INTEGER PRIMARY KEY (Auto Increment)
comment_id    INTEGER NOT NULL (FK -> fp_440.comment_id)
username      VARCHAR(50) NOT NULL (FK -> fp_100.username)
content       TEXT NOT NULL
created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
use_yn        VARCHAR(1) NOT NULL DEFAULT 'Y'
deleted_at    TIMESTAMP (소프트 삭제 시각)
```

### 4.2 소프트 삭제 방식
- `use_yn`: 'Y' (활성) / 'N' (비활성/삭제)
- `deleted_at`: 삭제 처리 시각 기록
- 조회 시 `use_yn='Y' AND deleted_at IS NULL` 조건으로 필터링
- 댓글 삭제 시 해당 댓글의 모든 답글도 함께 소프트 삭제

### 4.3 작성자 정보 (UserProfile)
```typescript
interface UserProfile {
  username: string;        // 사용자명
  userId: number;          // 사용자 ID
  nickName: string | null; // 닉네임
  profileImageUrl: string | null; // 프로필 이미지 URL
  isPrivate: boolean | null;      // 비공개 계정 여부
}
```

---

## 5. UI 구현 예시

### 5.1 React 댓글 컴포넌트 예시

```javascript
import { useState, useEffect } from 'react';

function CommentSection({ storeId, accessToken }) {
  const [comments, setComments] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(false);

  // 댓글 목록 로드
  useEffect(() => {
    loadComments();
  }, [storeId, page]);

  const loadComments = async () => {
    try {
      const response = await fetch(
        `/api/stores/${storeId}/comments?page=${page}&size=20`
      );
      const result = await response.json();

      if (result.success) {
        setComments(result.data.items);
        setTotal(result.data.total);
      }
    } catch (error) {
      console.error('댓글 로드 실패:', error);
    }
  };

  // 댓글 작성
  const handleSubmitComment = async () => {
    if (!newComment.trim()) return;
    if (newComment.length > 2000) {
      alert('댓글은 2000자를 초과할 수 없습니다.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`/api/stores/${storeId}/comments`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content: newComment })
      });

      const result = await response.json();
      if (result.success) {
        setNewComment('');
        loadComments(); // 댓글 목록 새로고침
      }
    } catch (error) {
      console.error('댓글 작성 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  // 댓글 삭제
  const handleDeleteComment = async (commentId) => {
    if (!confirm('댓글을 삭제하시겠습니까? (답글도 함께 삭제됩니다)')) {
      return;
    }

    try {
      const response = await fetch(`/api/comments/${commentId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${accessToken}`
        }
      });

      const result = await response.json();
      if (result.success) {
        loadComments(); // 댓글 목록 새로고침
      }
    } catch (error) {
      console.error('댓글 삭제 실패:', error);
    }
  };

  return (
    <div className="comment-section">
      {/* 댓글 작성 폼 */}
      <div className="comment-form">
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="댓글을 입력하세요..."
          maxLength={2000}
        />
        <button onClick={handleSubmitComment} disabled={loading}>
          {loading ? '작성 중...' : '댓글 작성'}
        </button>
        <span>{newComment.length}/2000</span>
      </div>

      {/* 댓글 목록 */}
      <div className="comment-list">
        <h3>댓글 {total}개</h3>
        {comments.map(comment => (
          <CommentItem
            key={comment.commentId}
            comment={comment}
            accessToken={accessToken}
            onDelete={handleDeleteComment}
          />
        ))}
      </div>

      {/* 페이지네이션 */}
      {total > 20 && (
        <div className="pagination">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            이전
          </button>
          <span>페이지 {page + 1}</span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={(page + 1) * 20 >= total}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}

// 개별 댓글 컴포넌트
function CommentItem({ comment, accessToken, onDelete }) {
  const [showReplies, setShowReplies] = useState(false);
  const [replies, setReplies] = useState([]);
  const [newReply, setNewReply] = useState('');

  // 답글 목록 로드
  const loadReplies = async () => {
    try {
      const response = await fetch(
        `/api/comments/${comment.commentId}/replies?page=0&size=50`
      );
      const result = await response.json();

      if (result.success) {
        setReplies(result.data.items);
        setShowReplies(true);
      }
    } catch (error) {
      console.error('답글 로드 실패:', error);
    }
  };

  // 답글 작성
  const handleSubmitReply = async () => {
    if (!newReply.trim()) return;

    try {
      const response = await fetch(
        `/api/comments/${comment.commentId}/replies`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ content: newReply })
        }
      );

      const result = await response.json();
      if (result.success) {
        setNewReply('');
        loadReplies(); // 답글 목록 새로고침
      }
    } catch (error) {
      console.error('답글 작성 실패:', error);
    }
  };

  return (
    <div className="comment-item">
      <div className="comment-header">
        <img src={comment.author.profileImageUrl} alt="profile" />
        <span className="nickname">{comment.author.nickName}</span>
        <span className="username">@{comment.author.username}</span>
        <span className="time">{formatTime(comment.createdAt)}</span>
      </div>

      <div className="comment-content">
        {comment.content}
      </div>

      <div className="comment-actions">
        <button onClick={() => setShowReplies(!showReplies)}>
          답글 {comment.replyCount}개 {showReplies ? '숨기기' : '보기'}
        </button>
        <button onClick={() => onDelete(comment.commentId)}>
          삭제
        </button>
      </div>

      {/* 답글 섹션 */}
      {showReplies && (
        <div className="reply-section">
          <div className="reply-form">
            <input
              type="text"
              value={newReply}
              onChange={(e) => setNewReply(e.target.value)}
              placeholder="답글을 입력하세요..."
              maxLength={2000}
            />
            <button onClick={handleSubmitReply}>답글 작성</button>
          </div>

          <div className="reply-list">
            {replies.map(reply => (
              <div key={reply.replyId} className="reply-item">
                <span className="nickname">{reply.author.nickName}</span>
                <span className="content">{reply.content}</span>
                <span className="time">{formatTime(reply.createdAt)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function formatTime(datetime) {
  const now = new Date();
  const created = new Date(datetime);
  const diff = Math.floor((now - created) / 1000); // 초 단위

  if (diff < 60) return '방금 전';
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`;

  return created.toLocaleDateString();
}
```

---

## 6. 테스트 시나리오

### 시나리오 1: 댓글 작성 및 조회
1. `POST /api/stores/337/comments` → `{"commentId": 1234}`
2. `GET /api/stores/337/comments?page=0&size=20` → 작성한 댓글이 목록에 표시됨

### 시나리오 2: 답글 작성 및 조회
1. `POST /api/comments/1234/replies` → `{"replyId": 5678}`
2. `GET /api/comments/1234/replies?page=0&size=50` → 작성한 답글이 목록에 표시됨
3. `GET /api/stores/337/comments?page=0&size=20` → 댓글의 `replyCount`가 1 증가

### 시나리오 3: 댓글 수정
1. `PUT /api/comments/1234` with `{"content": "수정된 내용"}`
2. `GET /api/stores/337/comments` → 댓글 내용이 수정되고 `updatedAt` 업데이트됨

### 시나리오 4: 댓글 삭제 (답글도 함께 삭제)
1. `DELETE /api/comments/1234`
2. `GET /api/stores/337/comments` → 해당 댓글이 목록에서 사라짐
3. `GET /api/comments/1234/replies` → `404 Not Found` (부모 댓글이 삭제됨)

### 시나리오 5: 권한 오류
1. 다른 사용자의 댓글 수정 시도 → `403 Forbidden`
2. 다른 사용자의 답글 삭제 시도 → `403 Forbidden`

---

## 7. 주요 특징

### 7.1 댓글/답글 분리 조회
- 댓글 목록 조회 시 답글은 포함하지 않고 `replyCount`만 반환
- 답글은 사용자가 "답글 보기" 버튼을 클릭할 때 별도 API로 로드
- 초기 로딩 속도 향상 및 불필요한 데이터 전송 방지

### 7.2 소프트 삭제
- 댓글/답글 삭제 시 물리적으로 삭제하지 않음
- `use_yn='N'`, `deleted_at` 설정으로 소프트 삭제 처리
- 데이터 복구 가능성 유지

### 7.3 연쇄 삭제
- 댓글 삭제 시 해당 댓글의 모든 답글도 자동으로 소프트 삭제
- 데이터 일관성 유지

### 7.4 작성자 정보
- 댓글/답글 조회 시 작성자의 프로필 정보 포함
- 사용자명, 닉네임, 프로필 이미지, 비공개 여부 제공

### 7.5 페이지네이션
- 댓글: 기본 20개, 최대 50개
- 답글: 기본 50개, 최대 50개
- 대규모 댓글/답글도 효율적으로 처리

---

## 8. 참고사항

### 8.1 인증
- 댓글/답글 조회는 인증 불필요 (공개 API)
- 댓글/답글 작성, 수정, 삭제는 Bearer Token 인증 필요

### 8.2 권한
- 수정/삭제는 본인이 작성한 댓글/답글에만 가능
- 권한이 없을 경우 `403 Forbidden` 에러 반환

### 8.3 정렬
- 댓글: 최신순 (comment_id DESC)
- 답글: 오래된 순 (reply_id ASC)

### 8.4 제약사항
- 댓글/답글 내용: 1~2000자
- 페이지 크기: 댓글 최대 50개, 답글 최대 50개
- 삭제된 댓글에는 답글 작성 불가

### 8.5 성능 최적화
- N+1 문제 방지: 작성자 정보를 한 번에 조회
- 답글 지연 로딩: 사용자가 요청할 때만 로드
- 인덱스: store_id, comment_id에 인덱스 적용
