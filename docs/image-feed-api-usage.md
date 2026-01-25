# Image Feed API Usage (Frontend)

Base: `/api`
Auth: `Authorization: Bearer {access_token}`

## 1) Create Image Feed

Endpoint: `POST /api/image-feeds`
Content-Type: `multipart/form-data`

Required:
- `files` (image files, multiple)
- `content`
- `address`

Optional:
- `storeName`
- `placeId`
- `withFriends` (string)
- `openYn` (default `Y`)
- `useYn` (default `Y`)

### Fetch (browser)
```javascript
async function createImageFeed({
  files,
  content,
  address,
  storeName,
  placeId,
  withFriends,
  openYn = "Y",
  useYn = "Y",
  accessToken
}) {
  const form = new FormData();
  files.forEach(file => form.append("files", file));
  form.append("content", content);
  form.append("address", address);
  if (storeName) form.append("storeName", storeName);
  if (placeId) form.append("placeId", placeId);
  if (withFriends) form.append("withFriends", withFriends);
  if (openYn) form.append("openYn", openYn);
  if (useYn) form.append("useYn", useYn);

  const res = await fetch("/api/image-feeds", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`
    },
    body: form
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || "create image feed failed");
  }
  return res.json();
}
```

## 2) Update Image Feed (text only)

Endpoint: `PATCH /api/image-feeds/{feedId}`
Content-Type: `application/json`

Required:
- `content`
- `address`

Optional:
- `storeName`
- `placeId`
- `withFriends`
- `openYn`
- `useYn`

### Fetch (browser)
```javascript
async function updateImageFeed(feedId, body, accessToken) {
  const res = await fetch(`/api/image-feeds/${feedId}`, {
    method: "PATCH",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || "update image feed failed");
  }
  return res.json();
}
```

## 3) Add Images to Feed

Endpoint: `POST /api/image-feeds/{feedId}/images`
Content-Type: `multipart/form-data`

Required:
- `files`

### Fetch (browser)
```javascript
async function addImageFeedImages(feedId, files, accessToken) {
  const form = new FormData();
  files.forEach(file => form.append("files", file));

  const res = await fetch(`/api/image-feeds/${feedId}/images`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`
    },
    body: form
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || "add images failed");
  }
  return res.json();
}
```

## 4) Delete Image Feed

Endpoint: `DELETE /api/image-feeds/{feedId}`

### Fetch (browser)
```javascript
async function deleteImageFeed(feedId, accessToken) {
  const res = await fetch(`/api/image-feeds/${feedId}`, {
    method: "DELETE",
    headers: {
      "Authorization": `Bearer ${accessToken}`
    }
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || "delete image feed failed");
  }
  return res.json();
}
```

## Curl snippets

```bash
curl -X POST "https://foodplayserver.shop/api/image-feeds" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "files=@/path/a.jpg" \
  -F "files=@/path/b.jpg" \
  -F "content=hello" \
  -F "address=Seoul"
```

```bash
curl -X PATCH "https://foodplayserver.shop/api/image-feeds/123" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"updated","address":"Seoul"}'
```

```bash
curl -X POST "https://foodplayserver.shop/api/image-feeds/123/images" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "files=@/path/c.jpg"
```

```bash
curl -X DELETE "https://foodplayserver.shop/api/image-feeds/123" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```
