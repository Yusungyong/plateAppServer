# Account Delete Frontend Guide

This document explains how the frontend should call the account deletion APIs.

Scope
- normal ID/password account deletion
- social account deletion for `apple`, `google`, `kakao`

## Summary

Use different endpoints based on account type.

- normal account: `DELETE /api/users/me`
- social account: `DELETE /api/users/me/social`

Both APIs require:
- `Authorization: Bearer {accessToken}`

## 1. Normal Account Delete

Endpoint
- `DELETE /api/users/me`

Request body
```json
{
  "password": "current-password",
  "reason": "optional"
}
```

Success response
```json
{
  "success": true,
  "message": "Account deleted successfully",
  "errorCode": null
}
```

Typical error response
```json
{
  "success": false,
  "message": "Password does not match",
  "errorCode": "INVALID_PASSWORD"
}
```

Notes
- `password` is required
- this API is only for normal accounts
- if the current user is a social account, this API returns `UNSUPPORTED_ACCOUNT_TYPE`

## 2. Social Account Delete

Endpoint
- `DELETE /api/users/me/social`

Request body depends on provider.

### Apple

```json
{
  "provider": "apple",
  "identityToken": "apple-identity-token",
  "authorizationCode": "apple-authorization-code",
  "reason": "optional"
}
```

### Google

```json
{
  "provider": "google",
  "idToken": "google-id-token",
  "reason": "optional"
}
```

### Kakao

```json
{
  "provider": "kakao",
  "accessToken": "kakao-access-token",
  "reason": "optional"
}
```

Success response
```json
{
  "success": true,
  "message": "Social account deleted successfully",
  "errorCode": null
}
```

Typical error responses
```json
{
  "success": false,
  "message": "provider is required",
  "errorCode": "INVALID_REQUEST"
}
```

```json
{
  "success": false,
  "message": "Provider does not match current social account",
  "errorCode": "SOCIAL_PROVIDER_MISMATCH"
}
```

```json
{
  "success": false,
  "message": "Social re-authentication failed",
  "errorCode": "SOCIAL_REAUTH_FAILED"
}
```

Notes
- use this API only for social accounts
- the backend checks the current logged-in user from the app access token
- the backend re-validates the provider token and matches it against the current user's `fp_110` social mapping

## 3. Frontend Decision Rule

The frontend should branch by account type.

- normal account: call `DELETE /api/users/me`
- Apple social account: call `DELETE /api/users/me/social` with `provider = "apple"`
- Google social account: call `DELETE /api/users/me/social` with `provider = "google"`
- Kakao social account: call `DELETE /api/users/me/social` with `provider = "kakao"`

Important
- do not call `DELETE /api/users/me` for a social account
- if you call the normal delete API without `password`, the backend returns `400 BAD_REQUEST`

## 4. Recommended Frontend Flow

### Normal account

1. user enters current password
2. frontend calls `DELETE /api/users/me`
3. on success, clear local tokens and session
4. move user to logged-out state

### Social account

1. frontend detects provider
2. frontend performs provider re-authentication
3. frontend gets provider token
4. frontend calls `DELETE /api/users/me/social`
5. on success, clear local tokens and session
6. move user to logged-out state

## 5. Fetch Examples

### Normal account

```js
export async function deleteMyAccount(password, reason, accessToken) {
  return api("/api/users/me", {
    method: "DELETE",
    token: accessToken,
    body: JSON.stringify({
      password,
      reason
    })
  });
}
```

### Social account

```js
export async function deleteMySocialAccount(payload, accessToken) {
  return api("/api/users/me/social", {
    method: "DELETE",
    token: accessToken,
    body: JSON.stringify(payload)
  });
}
```

### Example branch

```js
if (loginType === "normal") {
  await deleteMyAccount(password, reason, accessToken);
}

if (loginType === "google") {
  await deleteMySocialAccount(
    {
      provider: "google",
      idToken,
      reason
    },
    accessToken
  );
}

if (loginType === "kakao") {
  await deleteMySocialAccount(
    {
      provider: "kakao",
      accessToken: kakaoAccessToken,
      reason
    },
    accessToken
  );
}

if (loginType === "apple") {
  await deleteMySocialAccount(
    {
      provider: "apple",
      identityToken,
      authorizationCode,
      reason
    },
    accessToken
  );
}
```
