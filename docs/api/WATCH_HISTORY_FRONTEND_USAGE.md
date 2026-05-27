# Watch History Frontend Usage (Algorithm Data Collection)

This doc explains how the frontend should call the fp_305 watch history APIs
to collect viewing history data for recommendation algorithms.

This is NOT a resume playback (continue watching) guide.

Base URL: `/api`
Auth: `Authorization: Bearer {access_token}`
Content-Type: `application/json`

## 1) Start watch
Endpoint:
```
POST /api/videos/{storeId}/watch/start
```

Request:
```json
{
  "deviceInfo": "Web / Chrome 120.0",
  "videoQuality": "1080p",
  "sessionId": "optional-existing-session-id"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "watchId": 12345,
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "storeId": 337,
    "startedAt": "2026-01-18T10:00:00Z"
  },
  "error": null
}
```

Notes:
- If `sessionId` is omitted, the server generates one.
- Save `sessionId` in the player state.

## 2) Update progress
Endpoint:
```
PUT /api/videos/{storeId}/watch/progress
```

Request:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "durationWatched": 45,
  "videoQuality": "1080p"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "watchId": 12345,
    "durationWatched": 45,
    "completionRate": 0.25
  },
  "error": null
}
```

Notes:
- Call every 5-10 seconds or on key events (pause/seek/end).

## 3) Complete watch
Endpoint:
```
POST /api/videos/{storeId}/watch/complete
```

Request:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "durationWatched": 180,
  "completionStatus": true
}
```

Response:
```json
{
  "success": true,
  "data": {
    "watchId": 12345,
    "completed": true,
    "durationWatched": 180
  },
  "error": null
}
```

## 4) Watch info for analysis (optional)
Endpoint:
```
GET /api/videos/{storeId}/watch-info
```

Response:
```json
{
  "success": true,
  "data": {
    "hasWatched": true,
    "lastWatchedAt": "2026-01-18T10:00:00Z",
    "durationWatched": 45,
    "videoDuration": 180,
    "completionRate": 0.25,
    "completed": false,
    "canResume": true
  },
  "error": null
}
```

## Suggested client flow (data collection)
1) On play: call start and store sessionId.
2) On progress (5-10s or on pause/seek): call progress.
3) On end: call complete with final durationWatched.
