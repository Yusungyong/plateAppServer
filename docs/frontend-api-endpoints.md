# PlateApp Frontend API Endpoints

All endpoints are relative to `API_BASE_URL` and already include the `/api` prefix.

## Frontend Call Pattern

```js
const API_BASE_URL = process.env.API_BASE_URL;

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
    headers,
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res.json();
}
```

## Auth

- POST `/api/auth/signup`
- POST `/api/auth/login`
- POST `/api/auth/refresh`
- POST `/api/auth/social/apple`
- POST `/api/auth/social/kakao`
- POST `/api/auth/social/google`
- POST `/api/auth/reset-password`
- GET `/api/me`

See also: [docs/web-login-guide.md](/workspace/plate-main/docs/web-login-guide.md)

## Blocks

- POST `/api/blocks`
- GET `/api/blocks`
- DELETE `/api/blocks/{blockedUsername}`

## Email

- POST `/api/email/send-verification`
- POST `/api/email/verify`
- POST `/api/email/find-id`

## Health

- GET `/api/health`

## FAQs

- GET `/api/faqs`
- GET `/api/faqs/{faqId}`
- POST `/api/faqs`
- PATCH `/api/faqs/{faqId}`
- DELETE `/api/faqs/{faqId}`

### FAQ list

`GET /api/faqs`

Query params
- `category` (string, optional)
- `keyword` (string, optional, title search)
- `page` (number, optional, default 0)
- `size` (number, optional, default 10)

Example request
```js
export async function fetchFaqs({ category, keyword, page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  if (keyword) params.set("keyword", keyword);
  params.set("page", String(page));
  params.set("size", String(size));

  return api(`/api/faqs?${params.toString()}`);
}
```

Example response
```json
{
  "content": [
    {
      "faqId": 12,
      "category": "account",
      "title": "How do I reset my password?",
      "answer": "Use the password reset menu from the login screen.",
      "username": "admin",
      "isPinned": true,
      "viewCount": 128,
      "displayOrder": 1,
      "statusCode": "published",
      "createdAt": "2026-03-22T10:00:00",
      "updatedAt": "2026-03-22T10:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "hasNext": true
}
```

Frontend usage
```js
const faqPage = await fetchFaqs({ category: "account", keyword: "password" });

faqPage.content.forEach((faq) => {
  console.log(faq.title);
  console.log(faq.answer);
});
```

### FAQ detail

`GET /api/faqs/{faqId}`

Notes
- Detail response shape is the same as a single FAQ item in the list.
- `viewCount` is increased when this API is called.

Example request
```js
export async function fetchFaqDetail(faqId) {
  return api(`/api/faqs/${faqId}`);
}
```

Example response
```json
{
  "faqId": 12,
  "category": "account",
  "title": "How do I reset my password?",
  "answer": "Use the password reset menu from the login screen.",
  "username": "admin",
  "isPinned": true,
  "viewCount": 129,
  "displayOrder": 1,
  "statusCode": "published",
  "createdAt": "2026-03-22T10:00:00",
  "updatedAt": "2026-03-22T10:00:00"
}
```

Accordion-style example
```js
const faqList = await fetchFaqs({ page: 0, size: 10 });

const items = faqList.content.map((faq) => ({
  id: faq.faqId,
  label: `[${faq.category}] ${faq.title}`,
  content: faq.answer,
  meta: {
    username: faq.username,
    viewCount: faq.viewCount,
    updatedAt: faq.updatedAt,
    isPinned: faq.isPinned
  }
}));
```

### FAQ create

`POST /api/faqs`

Headers
- `Authorization: Bearer {accessToken}`
- admin token required

Request body
```json
{
  "category": "notice",
  "title": "Service usage guide",
  "answer": "FAQ content goes here.",
  "isPinned": true,
  "displayOrder": 1,
  "statusCode": "published"
}
```

Example request
```js
export async function createFaq(payload, accessToken) {
  return api("/api/faqs", {
    method: "POST",
    token: accessToken,
    body: JSON.stringify(payload)
  });
}
```

Example response
```json
{
  "faqId": 101,
  "category": "notice",
  "title": "Service usage guide",
  "answer": "FAQ content goes here.",
  "username": "admin",
  "isPinned": true,
  "viewCount": 0,
  "displayOrder": 1,
  "statusCode": "published",
  "createdAt": "2026-03-28T18:00:00",
  "updatedAt": "2026-03-28T18:00:00"
}
```

### FAQ update

`PATCH /api/faqs/{faqId}`

Headers
- `Authorization: Bearer {accessToken}`
- admin token required

Request body
```json
{
  "category": "notice",
  "title": "Updated guide title",
  "answer": "Updated FAQ content.",
  "isPinned": false,
  "displayOrder": 2,
  "statusCode": "published"
}
```

Example request
```js
export async function updateFaq(faqId, payload, accessToken) {
  return api(`/api/faqs/${faqId}`, {
    method: "PATCH",
    token: accessToken,
    body: JSON.stringify(payload)
  });
}
```

Example response
```json
{
  "faqId": 101,
  "category": "notice",
  "title": "Updated guide title",
  "answer": "Updated FAQ content.",
  "username": "admin",
  "isPinned": false,
  "viewCount": 15,
  "displayOrder": 2,
  "statusCode": "published",
  "createdAt": "2026-03-28T18:00:00",
  "updatedAt": "2026-03-28T18:30:00"
}
```

### FAQ delete

`DELETE /api/faqs/{faqId}`

Headers
- `Authorization: Bearer {accessToken}`
- admin token required

Example request
```js
export async function deleteFaq(faqId, accessToken) {
  const res = await fetch(`${API_BASE_URL}/api/faqs/${faqId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
}
```

Response
- `204 No Content`

## Search

- GET `/api/search/suggest`
- GET `/api/search`

## Map

- GET `/api/map/stores/nearby`
- GET `/api/map/stores/search`
- GET `/api/map/stores/suggest`

## Home

- GET `/api/home/image-thumbnails`
- GET `/api/home/random-candidates/recent`
- GET `/api/home/random-candidates/nearby`
- GET `/api/home/video-thumbnails`
- POST `/api/home/video-watch-history`
- GET `/api/home/feed`

## Image Feeds

- GET `/api/image-feeds/{feedId}`
- GET `/api/image-feeds/context`
- GET `/api/image-feeds/groups/`
- GET `/api/image-feeds/groups/{groupId}/images`
- POST `/api/image-feeds` (multipart/form-data)
- PATCH `/api/image-feeds/{feedId}` (application/json)
- POST `/api/image-feeds/{feedId}/images` (multipart/form-data)
- DELETE `/api/image-feeds/{feedId}`

### Image Feed Comments

- GET `/api/image-feeds/{feedId}/comments`
- POST `/api/image-feeds/{feedId}/comments`
- PUT `/api/image-feeds/comments/{commentId}`
- DELETE `/api/image-feeds/comments/{commentId}`
- GET `/api/image-feeds/comments/{commentId}/replies`
- POST `/api/image-feeds/comments/{commentId}/replies`
- PUT `/api/image-feeds/replies/{replyId}`
- DELETE `/api/image-feeds/replies/{replyId}`

## Store Comments

- GET `/api/stores/{storeId}/comments`
- POST `/api/stores/{storeId}/comments`
- GET `/api/comments/{commentId}/replies`
- POST `/api/comments/{commentId}/replies`
- PUT `/api/comments/{commentId}`
- DELETE `/api/comments/{commentId}`
- PUT `/api/replies/{replyId}`
- DELETE `/api/replies/{replyId}`

## Likes

### Image Feed Likes
- POST `/api/image-feeds/{feedId}/likes/toggle`
- GET `/api/image-feeds/{feedId}/likes/users`

### Store Likes
- POST `/api/stores/{storeId}/likes/toggle`
- GET `/api/stores/{storeId}/likes/status`
- GET `/api/stores/{storeId}/likes/users`

### Legacy Store Likes
- POST `/api/likes/{storeId}`
- DELETE `/api/likes/{storeId}`
- GET `/api/likes/{storeId}/me`
- GET `/api/likes/{storeId}/count`
- GET `/api/likes/me`
- GET `/api/likes/{storeId}/users`

## Friends

### Friend Controller
- GET `/api/friends`
- GET `/api/friends/search`
- GET `/api/friends/{username}/visits`
- GET `/api/friends/{username}/recent-stores`
- GET `/api/friends/suggest`
- GET `/api/friends/stores/{storeId}/friend-visits`
- GET `/api/friends/{username}/stores/{storeId}/visits`
- POST `/api/friends`
- GET `/api/friends/{username}/{friendName}/visits`
- GET `/api/friends/{username}/scheduled-visits`
- PATCH `/api/friends/{id}/status`
- DELETE `/api/friends/{id}`

### Friend Management
- GET `/api/friends/manage`
- GET `/api/friends/manage/search`
- DELETE `/api/friends/manage/{userId}`
- GET `/api/friends/manage/requests/sent`
- GET `/api/friends/manage/requests/received`
- POST `/api/friends/manage/requests`
- DELETE `/api/friends/manage/requests/{requestId}`
- PUT `/api/friends/manage/requests/{requestId}/accept`
- PUT `/api/friends/manage/requests/{requestId}/reject`

### Store Visits
- GET `/api/stores/{storeId}/friend-visits`

## Menu

- GET `/api/menu/`

## Notifications

### List notifications

`GET /api/notifications`

Query params
- `limit` (number, optional, default 20)
- `offset` (number, optional, default 0)
- `unreadOnly` (boolean, optional, default false)

Response (list)
```json
{
  "items": [
    {
      "notificationId": 123,
      "userId": 45,
      "type": "COMMENT",
      "title": "???볤?",
      "message": "?띻만?숇떂???볤????④꼈?듬땲??",
      "targetType": "post",
      "targetId": 987,
      "isRead": false,
      "readAt": null,
      "createdAt": "2025-01-02T12:34:56Z",
      "actorUserId": 78,
      "actorUsername": "hong",
      "actorProfileImageUrl": "https://cdn.example.com/profile.jpg",
      "data": {
        "deepLink": "plateapp://post/987"
      }
    }
  ]
}
```

Notes
- App accepts list payloads as `items` or raw array (see `src/api/notificationsApi.ts`).
- `data.deepLink` is optional; `targetType/targetId` is the primary navigation.

### Unread count

`GET /api/notifications/unread-count`

Response
```json
{
  "count": 5
}
```

### Mark as read

`PUT /api/notifications/{notificationId}/read`

Response: `204 No Content` (or empty JSON OK)

### Mark all as read

`PUT /api/notifications/read-all`

Response: `204 No Content` (or empty JSON OK)

### Delete one

`DELETE /api/notifications/{notificationId}`

Response: `204 No Content` (or empty JSON OK)

### Delete all

`DELETE /api/notifications/all`

Response: `204 No Content` (or empty JSON OK)

### Enums

Notification `type` (string enum)
- `LIKE`
- `COMMENT`
- `REPLY`
- `FOLLOW`
- `MENTION`
- `SYSTEM`

### DB field mapping

- `notificationId` -> `fp_20.id`
- `userId` -> `fp_20.receiver_id`
- `actorUserId` -> `fp_20.sender_id`
- `type` -> `fp_20.type`
- `message` -> `fp_20.message`
- `targetType` -> `fp_20.target_type`
- `targetId` -> `fp_20.target_id`
- `isRead` -> `fp_20.is_read`
- `readAt` -> `fp_20.read_at` (optional; can be null)
- `createdAt` -> `fp_20.created_at`

### Implementation notes

- Sort by `createdAt` desc by default.
- If `unreadOnly=true`, return only `is_read=false`.
- `actor*` fields are optional for system notifications.

## Profile

### My Profile
- POST `/api/my/profile`

### Profile Activity Detail
- GET `/api/users/{username}/videos`
- GET `/api/users/{username}/images`
- GET `/api/users/{username}/likes/videos`
- GET `/api/users/{username}/likes/images`
- GET `/api/my/videos`
- GET `/api/my/images`
- GET `/api/my/likes/videos`
- GET `/api/my/likes/images`

### Profile Activity Summary
- GET `/api/users/{username}/activity-summary`
- GET `/api/my/activity-summary`

### Profile Detail
- GET `/api/users/{username}/profile-detail`
- GET `/api/my/profile-detail`

### User Profile (ProfileController)
- GET `/api/users/me`
- GET `/api/users/{username}`
- PUT `/api/users/me`
- POST `/api/users/me/profile-image` (multipart/form-data)
- DELETE `/api/users/me/profile-image`
- PUT `/api/users/me/password`
- GET `/api/users/me/stats`
- GET `/api/users/{username}/stats`
- DELETE `/api/users/me`

### User Detail (Admin/Extended)
- GET `/api/users/detail/{username}`
- GET `/api/users/detail/{username}/public-profile`
- PATCH `/api/users/detail/{username}/email`
- PATCH `/api/users/detail/{username}/phone`
- PATCH `/api/users/detail/{username}/role`
- PATCH `/api/users/detail/{username}/active-region`
- PATCH `/api/users/detail/{username}/profile-image` (multipart/form-data)
- PATCH `/api/users/detail/{username}/nickname`
- PATCH `/api/users/detail/{username}/code`
- PATCH `/api/users/detail/{username}/fcm-token`
- PATCH `/api/users/detail/{username}/privacy`

### Profile History
- POST `/api/users/{username}/profile-history`

## Reports

- POST `/api/reports`
- GET `/api/reports`

## Videos

- POST `/api/videos` (multipart/form-data)
- DELETE `/api/videos/{storeId}`
- PATCH `/api/videos/{storeId}` (multipart/form-data)
- PATCH `/api/videos/{storeId}` (application/json)
- POST `/api/places`
- POST `/api/friends/visits`
- PATCH `/api/friends/visits/{id}`
- DELETE `/api/friends/visits/{id}`

### Watch History
- POST `/api/videos/{storeId}/watch/start`
- PUT `/api/videos/{storeId}/watch/progress`
- POST `/api/videos/{storeId}/watch/complete`
- GET `/api/watch-history`
- GET `/api/videos/{storeId}/watch-info`
- GET `/api/videos/{storeId}/watch-stats`

## QnA

- GET `/api/qna`
- GET `/api/qna/{qnaId}`
- POST `/api/qna`
- PATCH `/api/qna/{qnaId}`

### QnA list

`GET /api/qna`

Query params
- `category` (string, optional)
- `statusCode` (string, optional, admin only)
- `page` (number, optional, default 0)
- `size` (number, optional, default 10)

Example request
```js
export async function fetchQna({ category, page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  params.set("page", String(page));
  params.set("size", String(size));

  return api(`/api/qna?${params.toString()}`);
}
```

Example response
```json
{
  "content": [
    {
      "qnaId": 21,
      "username": null,
      "guestName": "홍길동",
      "guestEmail": null,
      "category": "이용문의",
      "question": "콘텐츠 검수 결과는 어디에서 확인할 수 있나요?",
      "answer": "현재는 고객지원 메뉴 안에서 검수 흐름을 확인할 수 있습니다.",
      "statusCode": "answered",
      "isPublic": true,
      "createdAt": "2026-03-28T13:00:00",
      "updatedAt": "2026-03-28T15:00:00",
      "answeredAt": "2026-03-28T15:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "hasNext": true
}
```

### QnA detail

`GET /api/qna/{qnaId}`

Notes
- detail shape is the same as a single list item
- public response hides `guestEmail`
- admin can receive full detail including `guestEmail`

Example request
```js
export async function fetchQnaDetail(qnaId) {
  return api(`/api/qna/${qnaId}`);
}
```

### QnA create

`POST /api/qna`

Notes
- login is not required
- if logged in, backend fills `username` from token
- if not logged in, frontend should send `guestName` or `guestEmail`

Request body
```json
{
  "guestName": "홍길동",
  "guestEmail": "guest@example.com",
  "category": "이용문의",
  "question": "문의하실 내용을 자세히 남겨 주세요.",
  "isPublic": true
}
```

Example request
```js
export async function createQna(payload, accessToken) {
  return api("/api/qna", {
    method: "POST",
    token: accessToken,
    body: JSON.stringify(payload)
  });
}
```

Example response
```json
{
  "qnaId": 21,
  "username": null,
  "guestName": "홍길동",
  "guestEmail": "guest@example.com",
  "category": "이용문의",
  "question": "문의하실 내용을 자세히 남겨 주세요.",
  "answer": null,
  "statusCode": "received",
  "isPublic": true,
  "createdAt": "2026-03-28T13:00:00",
  "updatedAt": "2026-03-28T13:00:00",
  "answeredAt": null
}
```

### QnA answer/update

`PATCH /api/qna/{qnaId}`

Notes
- admin only

Request body
```json
{
  "answer": "현재는 고객지원 메뉴 안에서 검수 흐름을 확인할 수 있습니다.",
  "statusCode": "answered",
  "isPublic": true
}
```

Example request
```js
export async function updateQna(qnaId, payload, accessToken) {
  return api(`/api/qna/${qnaId}`, {
    method: "PATCH",
    token: accessToken,
    body: JSON.stringify(payload)
  });
}
```

## Member Monitoring

Admin-only endpoints
- GET `/api/admin/member-monitoring/summary`
- GET `/api/admin/member-monitoring/login-risks`
- GET `/api/admin/member-monitoring/profile-changes`
- GET `/api/admin/member-monitoring/risk-users`

Headers
- `Authorization: Bearer {accessToken}`
- admin token required (`ROLE_ADMIN`)

### Summary

`GET /api/admin/member-monitoring/summary`

Example request
```js
export async function fetchMemberMonitoringSummary(accessToken) {
  return api("/api/admin/member-monitoring/summary", {
    token: accessToken
  });
}
```

Example response
```json
{
  "totalUsers": 148320,
  "newUsersToday": 126,
  "activeUsers7d": 24981,
  "loginFailureRateToday": 3.8,
  "pendingRoleChanges": 7,
  "riskUsers24h": 12
}
```

### Login risks

`GET /api/admin/member-monitoring/login-risks`

Query params
- `limit` (number, optional, default 20)

Example request
```js
export async function fetchMemberLoginRisks(accessToken, { limit = 20 } = {}) {
  const params = new URLSearchParams({ limit: String(limit) });
  return api(`/api/admin/member-monitoring/login-risks?${params.toString()}`, {
    token: accessToken
  });
}
```

Example response
```json
{
  "items": [
    {
      "username": "guest_8821",
      "riskType": "LOGIN_FAILURE_BURST",
      "riskLabel": "Repeated login failures",
      "detail": "11 failed attempts in the last 24 hours, same IP 203.241.10.7",
      "ipAddress": "203.241.10.7",
      "deviceId": "web-browser",
      "score": 91,
      "lastOccurredAt": "2026-03-28T15:48:00"
    }
  ]
}
```

### Profile changes

`GET /api/admin/member-monitoring/profile-changes`

Query params
- `limit` (number, optional, default 20)

Example request
```js
export async function fetchMemberProfileChanges(accessToken, { limit = 20 } = {}) {
  const params = new URLSearchParams({ limit: String(limit) });
  return api(`/api/admin/member-monitoring/profile-changes?${params.toString()}`, {
    token: accessToken
  });
}
```

Example response
```json
{
  "items": [
    {
      "historyId": 991,
      "username": "review_manager",
      "changedField": "권한",
      "actor": "system",
      "createdAt": "2026-03-28T15:42:00"
    }
  ]
}
```

### Risk users

`GET /api/admin/member-monitoring/risk-users`

Query params
- `limit` (number, optional, default 20)

Example request
```js
export async function fetchMemberRiskUsers(accessToken, { limit = 20 } = {}) {
  const params = new URLSearchParams({ limit: String(limit) });
  return api(`/api/admin/member-monitoring/risk-users?${params.toString()}`, {
    token: accessToken
  });
}
```

Example response
```json
{
  "items": [
    {
      "username": "spam_guest_1",
      "reportCount": 6,
      "blockedCount": 14,
      "recentActivityLabel": "18 image feed posts in the last 24 hours",
      "recommendedAction": "Immediate account review",
      "score": 94
    }
  ]
}
```

Frontend usage
```js
const [summary, loginRisks, profileChanges, riskUsers] = await Promise.all([
  fetchMemberMonitoringSummary(accessToken),
  fetchMemberLoginRisks(accessToken, { limit: 20 }),
  fetchMemberProfileChanges(accessToken, { limit: 20 }),
  fetchMemberRiskUsers(accessToken, { limit: 20 })
]);
```


