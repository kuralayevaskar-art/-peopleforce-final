param(
  [string]$AppRoot = "C:\OscarHR",
  [string]$NginxRoot = "C:\nginx"
)

$ErrorActionPreference = "Stop"
$NginxExe = Join-Path $NginxRoot "nginx.exe"
$NginxConf = Join-Path $AppRoot "nginx\nginx.conf"

if (-not (Test-Path $NginxExe)) {
  throw "nginx.exe not found. Install nginx to C:\nginx first."
}

if (-not (Test-Path $NginxConf)) {
  throw "nginx config not found: $NginxConf"
}

$running = Get-Process -Name nginx -ErrorAction SilentlyContinue
if ($running) {
  Write-Host "nginx is already running."
  exit 0
}

Start-Process -FilePath $NginxExe -ArgumentList "-c `"$NginxConf`" -p `"$NginxRoot\`"" -WorkingDirectory $NginxRoot -WindowStyle Hidden
Write-Host "nginx started."
