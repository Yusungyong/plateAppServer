# PlateApp Web Login Guide

This guide explains how a web frontend should log in to the current PlateApp backend.

## Summary

The current backend uses:

- stateless JWT authentication
- `Authorization: Bearer {accessToken}` for authenticated requests
- JSON body refresh flow with `refreshToken`
- no session login
- no cookie-based auth

This means the web frontend should work the same way as the app:

1. call login API
2. receive `accessToken` and `refreshToken`
3. store them on the frontend
4. send `accessToken` in the `Authorization` header
5. call refresh API when access token expires

## Base URL

All endpoints are relative to `API_BASE_URL` and already include the `/api` prefix.

Example:

```js
const API_BASE_URL = process.env.API_BASE_URL;
```

## Login API

Endpoint:

`POST /api/auth/login`

Request body:

```json
{
  "username": "user@example.com",
  "password": "plain-password",
  "deviceId": "web-chrome",
  "deviceModel": "Chrome",
  "os": "Windows",
  "osVersion": "11",
  "appVersion": "web-1.0.0"
}
```

Notes:

- `username` is used as the login id
- in this project, signup stores email as both `username` and `email`
- device fields are optional, but sending web client info is useful for login history

Success response shape:

```json
{
  "success": true,
  "data": {
    "accessToken": "access-token-value",
    "refreshToken": "refresh-token-value"
  },
  "message": null,
  "errorCode": null,
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:00:00Z"
}
```

## Refresh API

Endpoint:

`POST /api/auth/refresh`

Request body:

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
  }
}
```

Important:

- when refresh succeeds, replace both stored tokens
- do not keep using the old refresh token

## Current User API

Endpoint:

`GET /api/me`

Headers:

```http
Authorization: Bearer {accessToken}
```

Success response:

```json
{
  "success": true,
  "data": {
    "username": "user@example.com",
    "nickname": "plate-user",
    "email": "user@example.com",
    "profileImageUrl": null,
    "role": "USER"
  }
}
```

Use `/api/me` after login or page refresh to verify that the current token is still valid.

## Recommended Frontend Structure

Recommended in-memory state:

```js
let accessToken = null;
let refreshToken = null;
```

Recommended persistence:

- simplest: store tokens in `localStorage`
- safer: keep `accessToken` in memory and persist only `refreshToken`

Because this backend does not currently issue auth cookies, the frontend must manage tokens directly.

## Minimal Auth Client

```js
const API_BASE_URL = process.env.API_BASE_URL;

let accessToken = null;
let refreshToken = null;

export function loadAuthState() {
  accessToken = localStorage.getItem("accessToken");
  refreshToken = localStorage.getItem("refreshToken");
}

export function saveAuthState(tokens) {
  accessToken = tokens.accessToken;
  refreshToken = tokens.refreshToken;
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
}

export function clearAuthState() {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
}

export async function login({ username, password }) {
  const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      username,
      password,
      deviceId: "web-browser",
      deviceModel: navigator.userAgent,
      os: "web",
      osVersion: navigator.platform,
      appVersion: "web-1.0.0"
    })
  });

  const json = await res.json();
  if (!res.ok || !json.success) {
    throw new Error(json.message || `HTTP ${res.status}`);
  }

  saveAuthState(json.data);
  return json.data;
}

export async function refreshAccessToken() {
  if (!refreshToken) {
    throw new Error("No refresh token");
  }

  const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      refreshToken
    })
  });

  const json = await res.json();
  if (!res.ok || !json.success) {
    clearAuthState();
    throw new Error(json.message || `HTTP ${res.status}`);
  }

  saveAuthState(json.data);
  return json.data.accessToken;
}

export async function api(path, options = {}) {
  const headers = {
    ...(options.headers || {})
  };

  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  if (accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`;
  }

  let res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (res.status === 401 && refreshToken) {
    const newAccessToken = await refreshAccessToken();
    headers["Authorization"] = `Bearer ${newAccessToken}`;

    res = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers
    });
  }

  const json = await res.json();
  if (!res.ok || !json.success) {
    throw new Error(json.message || `HTTP ${res.status}`);
  }

  return json.data;
}
```

## Login Page Example

```js
export async function submitLoginForm(username, password) {
  await login({ username, password });
  const me = await api("/api/me", { method: "GET" });
  return me;
}
```

## App Startup Flow

Recommended startup flow:

1. load tokens from storage
2. if no token exists, show logged-out UI
3. if `accessToken` exists, call `/api/me`
4. if `/api/me` returns `401`, try refresh
5. if refresh succeeds, call `/api/me` again
6. if refresh fails, clear tokens and move to login page

## Logout

There is currently no dedicated logout API in this backend.

For web logout:

1. remove `accessToken`
2. remove `refreshToken`
3. clear user state in frontend store
4. redirect to login page if needed

Example:

```js
export function logout() {
  clearAuthState();
}
```

## CORS and Cookies

Current backend CORS is permissive enough for bearer-token requests.

Current backend auth does not use:

- `HttpOnly` cookies
- session cookies
- CSRF-based login flow

So for now:

- send tokens in headers
- do not use `credentials: "include"` for auth

## Important Limitations

For web production, this structure works but has security tradeoffs.

Current limitations:

- refresh token is handled in JavaScript
- no `HttpOnly` cookie support
- frontend is responsible for token persistence and refresh flow

If the web service becomes production-critical, consider upgrading the backend to:

- keep `accessToken` in memory
- issue `refreshToken` as `HttpOnly Secure SameSite` cookie
- optionally support both cookie auth and bearer auth

## Quick Checklist

- call `POST /api/auth/login`
- store `accessToken` and `refreshToken`
- send `Authorization: Bearer {accessToken}`
- on `401`, call `POST /api/auth/refresh`
- replace both tokens after refresh
- call `GET /api/me` to restore login state
- clear tokens on logout

## Related Docs

- [login-api-checklist-response.md](/workspace/plate-main/docs/login-api-checklist-response.md)
