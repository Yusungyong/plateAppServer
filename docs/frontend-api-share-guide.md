# PlateApp Frontend API Share Guide

This document is the frontend-facing API summary for the current backend.

Scope:
- frontend request rules
- auth/token handling
- response/error patterns
- endpoint inventory by domain
- important request body examples

Base local URL:
- `http://localhost:8090`

All paths below already include the `/api` prefix.

## 1. Common Request Rules

### JSON request

Use:
- `Content-Type: application/json`

Example:

```js
const API_BASE_URL = "http://localhost:8090";

async function api(path, options = {}) {
  const headers = options.headers || {};

  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  if (options.token) {
    headers["Authorization"] = `Bearer ${options.token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  const isJson = res.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await res.json() : null;

  if (!res.ok) {
    throw body ?? new Error(`HTTP ${res.status}`);
  }

  return body;
}
```

### Multipart request

Do not set `Content-Type` manually when using `FormData`.

Example:

```js
async function upload(path, formData, accessToken) {
  return fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body: formData
  });
}
```

### Bearer auth

Protected APIs require:

```http
Authorization: Bearer {accessToken}
```

## 2. Response Patterns

The backend currently uses a mixed response style.

### A. Wrapped `ApiResponse`

Mostly used by:
- auth
- email
- some profile/account APIs

Success shape:

```json
{
  "success": true,
  "data": {},
  "message": null,
  "errorCode": null,
  "requestId": "optional",
  "timestamp": "2026-05-03T12:00:00Z"
}
```

Error shape:

```json
{
  "success": false,
  "data": null,
  "message": "Error message",
  "errorCode": "ERROR_CODE",
  "requestId": "optional",
  "timestamp": "2026-05-03T12:00:00Z"
}
```

### B. Raw DTO / Page response

Mostly used by:
- FAQ list/detail
- QnA list/detail
- notification list
- monitoring APIs

Example:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```

### C. Empty response

Some delete/update APIs return:
- `204 No Content`

Frontend should handle that as success without parsing JSON.

## 3. Authentication Flow

### Normal login

Endpoint:
- `POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "1234",
  "deviceId": "web-browser",
  "deviceModel": "Chrome",
  "os": "web",
  "osVersion": "Windows 11",
  "appVersion": "web-1.0.0"
}
```

Required:
- `username`
- `password`

Success:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  }
}
```

Frontend reads:
- `response.data.accessToken`
- `response.data.refreshToken`

### Refresh token

Endpoint:
- `POST /api/auth/refresh`

Request:

```json
{
  "refreshToken": "stored-refresh-token"
}
```

Replace both tokens when refresh succeeds.

### Social login

Endpoints:
- `POST /api/auth/social/kakao`
- `POST /api/auth/social/google`
- `POST /api/auth/social/apple`

Provider payloads:

Kakao:

```json
{
  "accessToken": "kakao-access-token"
}
```

Google:

```json
{
  "idToken": "google-id-token"
}
```

Apple:

```json
{
  "identityToken": "apple-identity-token",
  "authorizationCode": "optional",
  "user": "optional"
}
```

Social login success includes:
- `accessToken`
- `refreshToken`
- `user`

### Password reset

Endpoint:
- `POST /api/auth/reset-password`

Flow:
1. `POST /api/email/send-verification`
2. `POST /api/email/verify`
3. `POST /api/auth/reset-password`

Request:

```json
{
  "email": "user@example.com",
  "verificationCode": "123456",
  "newPassword": "new-password"
}
```

## 4. Endpoint Inventory

Below is the frontend-facing endpoint list grouped by domain.

### Auth

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/auth/signup` | No | normal signup |
| `POST` | `/api/auth/login` | No | normal login |
| `POST` | `/api/auth/refresh` | No | token refresh |
| `POST` | `/api/auth/social/kakao` | No | Kakao access token |
| `POST` | `/api/auth/social/google` | No | Google id token |
| `POST` | `/api/auth/social/apple` | No | Apple identity token |
| `POST` | `/api/auth/reset-password` | No | email verification code required |
| `GET` | `/api/me` | Yes | current user summary |

### Email

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/email/send-verification` | No | send email verification code |
| `POST` | `/api/email/verify` | No | verify email code |
| `POST` | `/api/email/find-id` | No | find login ID |

### Health

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/health` | No |

### FAQ

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/faqs` | No | paged FAQ list |
| `GET` | `/api/faqs/{faqId}` | No | detail, increments view count |
| `POST` | `/api/faqs` | Admin | create |
| `PATCH` | `/api/faqs/{faqId}` | Admin | update |
| `DELETE` | `/api/faqs/{faqId}` | Admin | returns `204` |

FAQ list query params:
- `category`
- `keyword`
- `page`
- `size`

### Search / Map / Home

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/search` | No | search |
| `GET` | `/api/search/suggest` | No | suggestions |
| `GET` | `/api/map/stores/nearby` | No | nearby stores |
| `GET` | `/api/map/stores/search` | No | store search |
| `GET` | `/api/map/stores/suggest` | No | map suggestions |
| `GET` | `/api/home/image-thumbnails` | No | home image thumbnails |
| `GET` | `/api/home/random-candidates/recent` | No | recent random candidates |
| `GET` | `/api/home/random-candidates/nearby` | No | nearby random candidates |
| `GET` | `/api/home/video-thumbnails` | No | home video thumbnails |
| `POST` | `/api/home/video-watch-history` | Yes | record watch history |
| `GET` | `/api/home/feed` | No | home feed |

### QnA

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/qna` | No | paged list |
| `GET` | `/api/qna/{qnaId}` | No | public hides `guestEmail` |
| `POST` | `/api/qna` | No | guest or logged-in create |
| `PATCH` | `/api/qna/{qnaId}` | Admin | answer/update |

Create request:

```json
{
  "guestName": "홍길동",
  "guestEmail": "guest@example.com",
  "category": "이용문의",
  "question": "문의 내용을 입력하세요.",
  "isPublic": true
}
```

### Notifications

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/notifications` | Yes | list |
| `GET` | `/api/notifications/unread-count` | Yes | unread count |
| `PUT` | `/api/notifications/{notificationId}/read` | Yes | mark one as read |
| `PUT` | `/api/notifications/read-all` | Yes | mark all as read |
| `DELETE` | `/api/notifications/{notificationId}` | Yes | delete one |
| `DELETE` | `/api/notifications/all` | Yes | delete all |

List query params:
- `limit`
- `offset`
- `unreadOnly`

### Blocks

| Method | Path | Auth |
|---|---|---|
| `POST` | `/api/blocks` | Yes |
| `GET` | `/api/blocks` | Yes |
| `DELETE` | `/api/blocks/{blockedUsername}` | Yes |

### Image feeds

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/image-feeds/{feedId}` | No | detail |
| `GET` | `/api/image-feeds/context` | No | context |
| `GET` | `/api/image-feeds/groups/` | No | group list |
| `GET` | `/api/image-feeds/groups/{groupId}/images` | No | images by group |
| `POST` | `/api/image-feeds` | Yes | multipart create |
| `PATCH` | `/api/image-feeds/{feedId}` | Yes | JSON update |
| `POST` | `/api/image-feeds/{feedId}/images` | Yes | multipart image add |
| `DELETE` | `/api/image-feeds/{feedId}` | Yes | delete |

Comments / replies:
- `GET /api/image-feeds/{feedId}/comments`
- `POST /api/image-feeds/{feedId}/comments`
- `PUT /api/image-feeds/comments/{commentId}`
- `DELETE /api/image-feeds/comments/{commentId}`
- `GET /api/image-feeds/comments/{commentId}/replies`
- `POST /api/image-feeds/comments/{commentId}/replies`
- `PUT /api/image-feeds/replies/{replyId}`
- `DELETE /api/image-feeds/replies/{replyId}`

### Store comments

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/stores/{storeId}/comments` | No |
| `POST` | `/api/stores/{storeId}/comments` | Yes |
| `GET` | `/api/comments/{commentId}/replies` | No |
| `POST` | `/api/comments/{commentId}/replies` | Yes |
| `PUT` | `/api/comments/{commentId}` | Yes |
| `DELETE` | `/api/comments/{commentId}` | Yes |
| `PUT` | `/api/replies/{replyId}` | Yes |
| `DELETE` | `/api/replies/{replyId}` | Yes |

### Likes

Image feed likes:
- `POST /api/image-feeds/{feedId}/likes/toggle`
- `GET /api/image-feeds/{feedId}/likes/users`

Store likes:
- `POST /api/stores/{storeId}/likes/toggle`
- `GET /api/stores/{storeId}/likes/status`
- `GET /api/stores/{storeId}/likes/users`

Legacy likes:
- `POST /api/likes/{storeId}`
- `DELETE /api/likes/{storeId}`
- `GET /api/likes/{storeId}/me`
- `GET /api/likes/{storeId}/count`
- `GET /api/likes/me`
- `GET /api/likes/{storeId}/users`

### Friends

Core friend APIs:
- `GET /api/friends`
- `GET /api/friends/search`
- `GET /api/friends/{username}/visits`
- `GET /api/friends/{username}/recent-stores`
- `GET /api/friends/suggest`
- `GET /api/friends/stores/{storeId}/friend-visits`
- `GET /api/friends/{username}/stores/{storeId}/visits`
- `POST /api/friends`
- `GET /api/friends/{username}/{friendName}/visits`
- `GET /api/friends/{username}/scheduled-visits`
- `PATCH /api/friends/{id}/status`
- `DELETE /api/friends/{id}`

Friend management:
- `GET /api/friends/manage`
- `GET /api/friends/manage/search`
- `DELETE /api/friends/manage/{userId}`
- `GET /api/friends/manage/requests/sent`
- `GET /api/friends/manage/requests/received`
- `POST /api/friends/manage/requests`
- `DELETE /api/friends/manage/requests/{requestId}`
- `PUT /api/friends/manage/requests/{requestId}/accept`
- `PUT /api/friends/manage/requests/{requestId}/reject`

### Videos / places / visits / watch history

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/videos` | Yes | multipart upload |
| `DELETE` | `/api/videos/{storeId}` | Yes | delete |
| `PATCH` | `/api/videos/{storeId}` | Yes | multipart or JSON |
| `POST` | `/api/places` | Yes | place create |
| `POST` | `/api/friends/visits` | Yes | create visit |
| `PATCH` | `/api/friends/visits/{id}` | Yes | update visit |
| `DELETE` | `/api/friends/visits/{id}` | Yes | delete visit |

Watch APIs:
- `POST /api/videos/{storeId}/watch/start`
- `PUT /api/videos/{storeId}/watch/progress`
- `POST /api/videos/{storeId}/watch/complete`
- `GET /api/watch-history`
- `GET /api/videos/{storeId}/watch-info`
- `GET /api/videos/{storeId}/watch-stats`

### Profile

Profile APIs:
- `POST /api/my/profile`
- `GET /api/users/{username}/videos`
- `GET /api/users/{username}/images`
- `GET /api/users/{username}/likes/videos`
- `GET /api/users/{username}/likes/images`
- `GET /api/my/videos`
- `GET /api/my/images`
- `GET /api/my/likes/videos`
- `GET /api/my/likes/images`
- `GET /api/users/{username}/activity-summary`
- `GET /api/my/activity-summary`
- `GET /api/users/{username}/profile-detail`
- `GET /api/my/profile-detail`
- `GET /api/users/me`
- `GET /api/users/{username}`
- `PUT /api/users/me`
- `POST /api/users/me/profile-image`
- `DELETE /api/users/me/profile-image`
- `PUT /api/users/me/password`
- `GET /api/users/me/stats`
- `GET /api/users/{username}/stats`
- `DELETE /api/users/me`
- `DELETE /api/users/me/social`

Admin/extended:
- `GET /api/users/detail/{username}`
- `GET /api/users/detail/{username}/public-profile`
- `PATCH /api/users/detail/{username}/email`
- `PATCH /api/users/detail/{username}/phone`
- `PATCH /api/users/detail/{username}/role`
- `PATCH /api/users/detail/{username}/active-region`
- `PATCH /api/users/detail/{username}/profile-image`
- `PATCH /api/users/detail/{username}/nickname`
- `PATCH /api/users/detail/{username}/code`
- `PATCH /api/users/detail/{username}/fcm-token`
- `PATCH /api/users/detail/{username}/privacy`
- `POST /api/users/{username}/profile-history`

Rules:
- `GET /api/users/detail/{username}/public-profile` requires auth
- all other `users/detail` and `profile-history` APIs require admin

### Reports

| Method | Path | Auth |
|---|---|---|
| `POST` | `/api/reports` | Yes |
| `GET` | `/api/reports` | Yes |

### Menu

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/menu/` | No |

### Admin member monitoring

All admin-only:
- `GET /api/admin/member-monitoring/summary`
- `GET /api/admin/member-monitoring/login-risks`
- `GET /api/admin/member-monitoring/profile-changes`
- `GET /api/admin/member-monitoring/risk-users`

Requires:
- `Authorization: Bearer {accessToken}`
- authority `ROLE_ADMIN`

## 5. Account Deletion Rules

### Normal account

Endpoint:
- `DELETE /api/users/me`

Request:

```json
{
  "password": "current-password",
  "reason": "optional"
}
```

### Social account

Endpoint:
- `DELETE /api/users/me/social`

Google:

```json
{
  "provider": "google",
  "idToken": "google-id-token",
  "reason": "optional"
}
```

Kakao:

```json
{
  "provider": "kakao",
  "accessToken": "kakao-access-token",
  "reason": "optional"
}
```

Apple:

```json
{
  "provider": "apple",
  "identityToken": "apple-identity-token",
  "authorizationCode": "apple-authorization-code",
  "reason": "optional"
}
```

## 6. Recommended Frontend Handling Rules

1. Treat auth APIs as wrapped `ApiResponse`.
2. Treat list/detail APIs as raw DTO/page unless the endpoint clearly documents `ApiResponse`.
3. Handle `204 No Content` separately.
4. Always store both `accessToken` and `refreshToken`.
5. On `401`, attempt refresh if refresh token exists.
6. For admin APIs, call only with admin token.
7. For multipart APIs, use `FormData` and do not set JSON content type.

## 7. Recommended Frontend Error Handling

Typical auth error shape:

```json
{
  "success": false,
  "data": null,
  "message": "Error message",
  "errorCode": "AUTH_401"
}
```

Common examples:
- `AUTH_401`: invalid login
- `AUTH_402`: expired access token
- `AUTH_411`: expired refresh token
- `AUTH_412`: invalid refresh token
- `INVALID_PASSWORD`: wrong current password
- `UNSUPPORTED_ACCOUNT_TYPE`: wrong delete/change-password flow
- `SOCIAL_PROVIDER_MISMATCH`: social provider mismatch
- `SOCIAL_REAUTH_FAILED`: social delete re-auth failed

## 8. Frontend Share Summary

If the frontend only needs the most important rules:

1. Base URL is `http://localhost:8090`
2. Protected APIs use `Authorization: Bearer {accessToken}`
3. Normal login uses `username` + `password`
4. Social login uses:
   - Kakao: `accessToken`
   - Google: `idToken`
   - Apple: `identityToken`
5. Normal account delete is `DELETE /api/users/me`
6. Social account delete is `DELETE /api/users/me/social`
7. FAQ/QnA admin APIs and member monitoring APIs require admin token
