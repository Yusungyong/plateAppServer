# Video Delete API Usage (Frontend Example)

Endpoint: `DELETE /api/videos/{storeId}`

Notes:
- Requires Authorization header (Bearer token).
- Returns `{ "ok": true }` on success.

## Fetch (Browser/React)

```javascript
async function deleteVideo(storeId, accessToken) {
  const res = await fetch(`/api/videos/${storeId}`, {
    method: "DELETE",
    headers: {
      "Authorization": `Bearer ${accessToken}`
    }
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || "delete failed");
  }

  return res.json();
}

// usage
// await deleteVideo(123, accessToken);
```

## Curl (for quick test)

```bash
curl -X DELETE "https://foodplayserver.shop/api/videos/123" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```
