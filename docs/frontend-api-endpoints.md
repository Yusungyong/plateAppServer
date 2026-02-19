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
