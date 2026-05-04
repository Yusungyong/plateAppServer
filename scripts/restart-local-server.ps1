Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$stdoutLog = Join-Path $projectRoot "server-restart.out.log"
$stderrLog = Join-Path $projectRoot "server-restart.err.log"

function Resolve-MavenCommand {
    $candidates = @(
        "C:\Users\su12n\.m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1dcac01d8862b2550b2939e09a0809bbc5182a5bc\bin\mvn.cmd",
        "C:\Users\su12n\.m2\wrapper\dists\apache-maven-3.9.11\a2d47e15\bin\mvn.cmd"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "Maven executable not found in local wrapper cache."
}

function Stop-PlateProcesses {
    $portProcesses = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($processId in $portProcesses) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    try {
        $javaProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
            Where-Object {
                $_.CommandLine -like "*com.plateapp.plate_main.PlateMainApplication*" -or
                $_.CommandLine -like "*$projectRoot*"
            }

        foreach ($proc in $javaProcesses) {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Warning "Could not enumerate Java command lines. Continuing with port-based cleanup only."
    }
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds,
        [int]$LauncherProcessId
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($listener) {
            return $listener | Select-Object -First 1
        }

        $proc = Get-Process -Id $LauncherProcessId -ErrorAction SilentlyContinue
        if (-not $proc) {
            return $null
        }

        Start-Sleep -Milliseconds 800
    }

    return $null
}

Stop-PlateProcesses
Start-Sleep -Seconds 2

if (Test-Path $stdoutLog) { Remove-Item $stdoutLog -Force }
if (Test-Path $stderrLog) { Remove-Item $stderrLog -Force }

$mvn = Resolve-MavenCommand
$arguments = @(
    "-o",
    "-Dspring-boot.run.profiles=local",
    "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false",
    "spring-boot:run"
)

$process = Start-Process `
    -FilePath $mvn `
    -ArgumentList $arguments `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

$listener = Wait-ForPort -Port 8090 -TimeoutSeconds 60 -LauncherProcessId $process.Id

if (-not $listener) {
    Write-Host "Server failed to open port 8090."
    Write-Host "STDOUT tail:"
    Get-Content $stdoutLog -Tail 120 -ErrorAction SilentlyContinue
    Write-Host "STDERR tail:"
    Get-Content $stderrLog -Tail 120 -ErrorAction SilentlyContinue
    exit 1
}

Write-Host "Server started successfully."
Write-Host "Launcher PID: $($process.Id)"
Write-Host "Server PID: $($listener.OwningProcess)"
Write-Host "URL: http://localhost:8090"
Write-Host "STDOUT: $stdoutLog"
Write-Host "STDERR: $stderrLog"
$listener | Select-Object LocalAddress, LocalPort, OwningProcess
