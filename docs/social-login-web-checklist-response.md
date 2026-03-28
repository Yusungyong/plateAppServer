# Social Login Web Checklist Response

This document answers the web frontend social login checklist based on the current backend implementation.

## Summary

Implemented endpoints:

- `POST /api/auth/social/kakao`
- `POST /api/auth/social/google`
- `POST /api/auth/social/apple`

Current backend expectation is not authorization-code exchange.

The frontend must first complete provider login and then send a provider-issued token to the backend.

## 1. Request Body By Provider

### Kakao

Endpoint:

- `POST /api/auth/social/kakao`

Actual request body:

```json
{
  "accessToken": "kakao-access-token"
}
```

Meaning:

- frontend must obtain Kakao `accessToken`
- backend calls Kakao user info API with that token

Not currently supported by this endpoint:

- OAuth `code`
- `redirectUri`

### Google

Endpoint:

- `POST /api/auth/social/google`

Actual request body:

```json
{
  "idToken": "google-id-token"
}
```

Meaning:

- frontend must obtain Google `idToken`
- backend verifies it against Google token info endpoint

Not currently supported by this endpoint:

- OAuth `code`
- `redirectUri`

### Apple

Endpoint:

- `POST /api/auth/social/apple`

Actual request body:

```json
{
  "identityToken": "apple-identity-token",
  "authorizationCode": "optional",
  "user": "optional"
}
```

Meaning:

- backend currently uses `identityToken`
- `authorizationCode` and `user` may be sent, but current login flow is based on `identityToken`

Not currently supported by this endpoint:

- authorization-code exchange with backend-only token swap

## 2. Response Format

Social login response is wrapped in `ApiResponse`, same as regular login.

But unlike regular login, social login currently includes a `user` object.

Actual response shape:

```json
{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "user": {
      "username": "google_123456",
      "email": "user@example.com",
      "nickname": "User Name",
      "profileImageUrl": null,
      "activeRegion": null
    }
  },
  "message": null,
  "errorCode": null,
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:00:00Z"
}
```

Frontend implication:

- social login can reuse the same token storage logic as regular login
- if needed, frontend may also use `response.data.user`

## 3. Web Client Settings Status

### Google

Backend currently reads:

- `google.client-id`

So backend-side token validation depends on a configured Google client ID.

### Apple

Backend currently reads:

- `apple.client-id`

So Apple token validation depends on a configured Apple client ID.

### Kakao

Backend currently does not validate an OAuth authorization code.
It expects a Kakao access token from the frontend.

There are Kakao-related config placeholders in `application.yaml`, but the current endpoint does not use a web authorization code exchange flow.

## 4. User Identification Policy

Current policy:

- social login success creates a new local user automatically if no mapping exists
- social account mapping is stored in `fp_110`
- lookup key is `(provider, provider_user_id)`

Important detail:

- current code does not merge automatically with an existing local account by email
- it creates a provider-based username such as `google_xxx`, `kakao_xxx`, `apple_xxx`

So the current behavior is:

1. check `fp_110` for existing `(provider, provider_user_id)`
2. if found, load linked local user
3. if not found, create a new local user
4. insert mapping row into `fp_110`

## 5. Error Response Format

Current backend behavior is not ideal yet for social login errors.

Important:

- provider token validation failures inside `SocialAuthService` currently throw `IllegalArgumentException`
- those exceptions are handled by the global fallback handler
- that means the frontend may currently receive a generic internal error response instead of a dedicated social auth error code

Likely current failure response shape:

```json
{
  "success": false,
  "data": null,
  "message": "server internal error message",
  "errorCode": "COMMON_500",
  "requestId": "optional-request-id",
  "timestamp": "2026-03-28T12:05:00Z"
}
```

So for frontend:

- do not assume a dedicated `AUTH_SOCIAL_401` contract exists yet
- current error contract for social login is not fully specialized

## Final Answer For Frontend

1. Kakao request body: `{ \"accessToken\": \"...\" }`
2. Google request body: `{ \"idToken\": \"...\" }`
3. Apple request body: `{ \"identityToken\": \"...\" }` and optionally `authorizationCode`, `user`
4. Social login does not currently use backend authorization-code exchange
5. Social login response is wrapped and returns `accessToken`, `refreshToken`, and `user`
6. New users are auto-created and linked through `fp_110`
7. Existing local accounts are not automatically merged by email
8. Social login error handling is not yet normalized to a dedicated social auth error code

## Recommended Frontend Implementation Order

1. Google first
2. Kakao next
3. Apple after web client/service ID configuration is confirmed

## Recommended Frontend Assumption

### Kakao

```json
{
  "accessToken": "provider-token"
}
```

### Google

```json
{
  "idToken": "provider-id-token"
}
```

### Apple

```json
{
  "identityToken": "provider-identity-token"
}
```
