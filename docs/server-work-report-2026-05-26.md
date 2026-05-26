# Server Work Report

Last updated: 2026-05-26

## Completed

### 1. Social signup flow split

- Existing social account: immediate login
- New social account: `signup_required` flow
- Added `POST /api/auth/social/signup/complete`
- Reused existing social identity table `fp_110`
- Added temporary signup session table DDL for `fp_111`

Related files:

- `src/main/java/com/plateapp/plate_main/auth/controller/AuthController.java`
- `src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java`
- `src/main/java/com/plateapp/plate_main/auth/dto/SocialAuthResponse.java`
- `src/main/java/com/plateapp/plate_main/auth/dto/SocialSignupCompleteRequest.java`
- `src/main/java/com/plateapp/plate_main/auth/domain/SocialSignupSession.java`
- `src/main/java/com/plateapp/plate_main/auth/repository/SocialSignupSessionRepository.java`
- `docs/social-signup-session-fp111-ddl.sql`
- `docs/social-signup-server-db-guide.md`

### 2. Social signup token persistence fix

- `POST /api/auth/social/signup/complete` now follows the same token issuance path as normal login
- Refresh token is persisted, so `/api/auth/refresh` works the same way for:
  - normal login
  - existing social login
  - social signup complete

Related file:

- `src/main/java/com/plateapp/plate_main/auth/service/SocialAuthService.java`

### 3. Profile API response standardization

Standardized the following endpoints to the common `common.api.ApiResponse` shape:

- `PUT /api/users/me/password`
- `DELETE /api/users/me`
- `DELETE /api/users/me/social`

Related file:

- `src/main/java/com/plateapp/plate_main/profile/controller/ProfileController.java`

### 4. Likes map API

Added place-based liked map API for frontend map mode:

- `GET /api/my/likes/places/map`

Response is grouped by place when possible and includes:

- `placeId`
- `storeId`
- `storeName`
- `address`
- `category`
- `lat`
- `lng`
- `thumbnailUrl`
- `videoLikeCount`
- `imageLikeCount`
- `totalLikeCount`
- `latestLikedAt`

Related files:

- `src/main/java/com/plateapp/plate_main/profile/controller/ProfileActivityDetailController.java`
- `src/main/java/com/plateapp/plate_main/profile/service/ProfileActivityDetailService.java`
- `src/main/java/com/plateapp/plate_main/profile/dto/LikedPlaceMapResponse.java`

## Current Next Task

### Viewer map focus support

Frontend requested stronger location guarantees for:

- `GET /api/image-feeds/{feedId}`
- image group summary responses
- `GET /api/home/feed`

Goal:

- ensure `placeId`
- ensure `lat`
- ensure `lng`
- use original content-linked place data rather than frontend search fallback

Current issue:

- some legacy contents still have missing `placeId`
- some responses already have the fields in DTOs, but services return `null` because fallback place resolution is missing

Planned direction:

- add server-side fallback place resolution using existing store/feed/place data
- keep response shapes unchanged
- improve location focus accuracy without forcing frontend search-based inference
