# Social Signup Server / DB Guide

## Summary

- Existing `fp_110` (`SocialAccount`) is reused as the social identity table.
- New `fp_111` is required for temporary signup sessions.
- Social first-login no longer auto-creates a user.
- New social users now receive `kind=signup_required` and must finish `POST /api/auth/social/signup/complete`.

## Required DB Work

1. Run [social-signup-session-fp111-ddl.sql](/c:/workspace/plate-main/docs/social-signup-session-fp111-ddl.sql)
2. No schema change is required for `fp_110`

## New Server Behavior

### `POST /api/auth/social/apple`
### `POST /api/auth/social/kakao`
### `POST /api/auth/social/google`

Two possible responses:

1. Existing linked user
- `kind = login_success`
- returns `accessToken`, `refreshToken`, `user`

2. New social identity
- `kind = signup_required`
- returns `signupToken`, `provider`, `providerUserId`, `email`, `nickname`

### `POST /api/auth/social/signup/complete`

Input:

- `signupToken`
- `email`
- `nickname`
- `agreeService`
- `agreePrivacy`

Behavior:

- validates session existence / expiry / consumed state
- validates required agreements
- creates `fp_100` user
- creates `fp_110` social identity row
- marks `fp_111.consumed_at`
- returns login tokens

## Current Validation Scope

- required agreements must be `true`
- `email` must be unique
- `signupToken` must exist and be unconsumed
- linked social identity must not already exist

## Known Limits

- nickname duplicate check is not added in this change
- agreement acceptance is validated but not separately persisted, matching the current general signup policy
