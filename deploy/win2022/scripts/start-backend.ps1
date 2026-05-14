param(
  [string]$AppRoot = "C:\OscarHR"
)

$ErrorActionPreference = "Stop"
$JarPath = Join-Path $AppRoot "backend\app.jar"
$ConfigPath = Join-Path $AppRoot "config\application-secrets.properties"
$LogDir = Join-Path $AppRoot "logs"
$OutLog = Join-Path $LogDir "backend-stdout.log"
$ErrLog = Join-Path $LogDir "backend-stderr.log"
$HealthUrl = "http://127.0.0.1:8080/api/v1/health"
$StartupTimeoutSeconds = 90

function Get-BackendProcess {
  $escapedJarPath = [WildcardPattern]::Escape($JarPath)
  Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -and $_.CommandLine -like "*$escapedJarPath*" }
}

function Test-BackendHealth {
  try {
    $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 3
    return ($response.success -eq $true)
  } catch {
    return $false
  }
}

function Show-LogTail {
  param([string]$Path, [string]$Title)

  if (Test-Path $Path) {
    Write-Host ""
    Write-Host $Title
    Get-Content $Path -Tail 80
  }
}

if (-not (Test-Path $JarPath)) {
  throw "Backend jar not found: $JarPath"
}

if (-not (Test-Path $ConfigPath)) {
  throw "Config not found: $ConfigPath"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$existing = Get-BackendProcess

if ($existing) {
  if (Test-BackendHealth) {
    Write-Host "Backend is running and healthy. PID: $($existing.ProcessId -join ', ')"
    exit 0
  }

  Write-Host "Backend process exists but health is not available. Restarting PID: $($existing.ProcessId -join ', ')"
  foreach ($process in $existing) {
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
  }
  Start-Sleep -Seconds 3
}

$arguments = @(
  "-jar", "`"$JarPath`"",
  "--spring.config.import=optional:file:$($ConfigPath.Replace('\','/'))"
)

Start-Process -FilePath "java.exe" `
  -ArgumentList $arguments `
  -WorkingDirectory $AppRoot `
  -RedirectStandardOutput $OutLog `
  -RedirectStandardError $ErrLog `
  -WindowStyle Hidden

Write-Host "Backend process started. Waiting for health..."

for ($i = 1; $i -le $StartupTimeoutSeconds; $i++) {
  if (Test-BackendHealth) {
    $running = Get-BackendProcess
    Write-Host "Backend is UP. PID: $($running.ProcessId -join ', ')"
    exit 0
  }

  $running = Get-BackendProcess
  if (-not $running) {
    Write-Host "Backend process exited during startup." -ForegroundColor Red
    Show-LogTail -Path $ErrLog -Title "backend-stderr.log:"
    Show-LogTail -Path $OutLog -Title "backend-stdout.log:"
    Show-LogTail -Path (Join-Path $LogDir "hr-platform-backend.log") -Title "hr-platform-backend.log:"
    exit 1
  }

  Start-Sleep -Seconds 1
}

Write-Host "Backend did not become healthy after $StartupTimeoutSeconds seconds." -ForegroundColor Red
Show-LogTail -Path $ErrLog -Title "backend-stderr.log:"
Show-LogTail -Path $OutLog -Title "backend-stdout.log:"
Show-LogTail -Path (Join-Path $LogDir "hr-platform-backend.log") -Title "hr-platform-backend.log:"
exit 1
