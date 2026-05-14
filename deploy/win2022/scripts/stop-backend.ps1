param(
  [string]$AppRoot = "C:\OscarHR"
)

$JarPath = Join-Path $AppRoot "backend\app.jar"
$escapedJarPath = [WildcardPattern]::Escape($JarPath)
$processes = Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -and $_.CommandLine -like "*$escapedJarPath*" }

foreach ($process in $processes) {
  Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
  Write-Host "Stopped backend PID $($process.ProcessId)"
}

if (-not $processes) {
  Write-Host "Backend was not running."
  exit 0
}

Start-Sleep -Seconds 3
$stillRunning = Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -and $_.CommandLine -like "*$escapedJarPath*" }

if ($stillRunning) {
  Write-Host "Backend still running PID: $($stillRunning.ProcessId -join ', ')" -ForegroundColor Yellow
  exit 1
}

Write-Host "Backend stopped."
