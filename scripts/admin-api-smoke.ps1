param(
    [Parameter(Mandatory = $true)][string]$BaseUrl,
    [string]$Origin = 'http://localhost:3001',
    [string]$AccessToken
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')
$preflight = Invoke-WebRequest -Method Options -Uri "$base/api/admin/stores" -Headers @{
    Origin = $Origin
    'Access-Control-Request-Method' = 'GET'
    'Access-Control-Request-Headers' = 'authorization,content-type'
}

if ($preflight.Headers['Access-Control-Allow-Origin'] -ne $Origin) {
    throw "CORS preflight did not allow $Origin"
}

if ($AccessToken) {
    $response = Invoke-WebRequest -Method Get -Uri "$base/api/admin/stores?page=0&size=1" -Headers @{
        Origin = $Origin
        Authorization = "Bearer $AccessToken"
    }
    if ($response.StatusCode -ne 200) {
        throw "Authenticated smoke request failed with $($response.StatusCode)"
    }
}

Write-Host "Admin API CORS smoke test passed for $Origin"
