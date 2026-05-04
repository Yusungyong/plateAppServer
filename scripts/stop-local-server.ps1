Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

try {
    $portProcesses = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($processId in $portProcesses) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
} catch {
}

try {
    $javaProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object {
            $_.CommandLine -like "*com.plateapp.plate_main.PlateMainApplication*" -or
            $_.CommandLine -like "*C:\workspace\plate-main*"
        }

    foreach ($proc in $javaProcesses) {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
} catch {
    Write-Warning "Could not enumerate Java command lines. Port-based cleanup only."
}

Write-Host "Local plate-main server processes stopped."
