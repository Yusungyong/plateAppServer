# Login API Checklist Response

This document answers the frontend login checklist based on the current backend implementation.

## 1. Login API URL

Actual login endpoint:

- `POST /api/auth/login`

Base local URL currently assumed by frontend is valid:

- `http://localhost:8090`

So the full local login URL is:

- `http://localhost:8090/api/auth/login`

## 2. Login Request Spec

Method:

- `POST`

Content-Type:

- `application/json`

Actual request JSON:

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

Required fields:

- `username`
- `password`

Optional fields:

- `deviceId`
- `deviceModel`
- `os`
- `osVersion`
- `appVersion`

Minimum valid request:

```json
{
  "username": "admin",
  "password": "1234"
}
```

Important:

- the backend expects `username`, not `loginId`
- in this project, normal signup stores the email value as both `username` and `email`

## 3. Login Response Spec

The regular login API currently returns tokens only.

Actual success response shape:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  },
  "message": null,
  "errorCode": null,
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:00:00Z"
}
```

Important correction for frontend:

- regular login does not currently return `user`
- regular login response is wrapped in `ApiResponse`
- so the frontend should read `response.data.accessToken`, not `response.accessToken`

Frontend parsing example:

```js
const json = await res.json();
const accessToken = json.data.accessToken;
const refreshToken = json.data.refreshToken;
```

## 4. Authentication Method

Current backend auth method:

- JWT Bearer auth

After login, protected API requests should send:

```http
Authorization: Bearer {accessToken}
```

The backend is not using:

- session login
- server session state
- cookie-based auth for login

## 5. Refresh Token API

Refresh token API exists.

Endpoint:

- `POST /api/auth/refresh`

Request:

```json
{
  "refreshToken": "stored-refresh-token"
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token"
  },
  "message": null,
  "errorCode": null,
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:10:00Z"
}
```

Important:

- when refresh succeeds, replace both tokens
- do not keep using the old refresh token

## 6. Logout API

Current backend status:

- no dedicated logout API is implemented

Current frontend logout handling:

- remove stored `accessToken`
- remove stored `refreshToken`
- clear authenticated user state

Note:

- because refresh tokens are stored in `fp_103`, a backend logout endpoint would be useful later
- but it does not exist in the current API contract

## 7. Permission Check for FAQ Write APIs

Current backend status:

- FAQ read APIs exist
- FAQ create/update/delete APIs do not currently exist

Implemented FAQ APIs:

- `GET /api/faqs`
- `GET /api/faqs/{faqId}`

So at this moment:

- there is no active backend permission contract yet for FAQ create/update/delete
- there is no implemented role check for FAQ admin actions

Role field does exist in `fp_100.role`, and code examples elsewhere use values such as:

- `USR`

But for FAQ write operations:

- no backend API
- no role gate
- no confirmed admin role contract yet

## 8. Login Failure Response Format

Login failures currently return HTTP `401`.

Actual response shape:

```json
{
  "success": false,
  "data": null,
  "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
  "errorCode": "AUTH_401",
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:05:00Z"
}
```

Notes:

- the response is wrapped in `ApiResponse`
- the key is `errorCode`, not `code`
- the key is `message`, not nested `error.message`

Other auth-related error cases:

- expired access token: `401`, `errorCode: "AUTH_402"`
- invalid refresh token: `401`, `errorCode: "AUTH_412"`
- expired refresh token: `401`, `errorCode: "AUTH_411"`

## Final Answers For Frontend

1. Login API URL is `POST /api/auth/login`
2. Login request JSON uses `username` and `password`, with optional device metadata
3. Login response JSON is wrapped and currently returns `accessToken` and `refreshToken` only
4. The backend uses JWT Bearer auth
5. Refresh API exists at `POST /api/auth/refresh`
6. Logout API does not currently exist
7. FAQ write/update/delete permission is not defined yet because those APIs are not implemented
8. Login failure returns HTTP `401` with `ApiResponse` format using `errorCode` and `message`

## Recommended Frontend Update

The frontend should update its temporary assumption from this:

```json
{
  "accessToken": "jwt-token",
  "user": {
    "username": "admin",
    "displayName": "admin"
  }
}
```

To this:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  }
}
```

Recommended frontend flow:

1. call `POST /api/auth/login`
2. read `response.data.accessToken`
3. read `response.data.refreshToken`
4. store both tokens
5. send `Authorization: Bearer {accessToken}` on protected requests
6. call `POST /api/auth/refresh` when access token expires
7. call `GET /api/me` to load current user profile after login
