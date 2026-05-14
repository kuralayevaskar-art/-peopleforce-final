param(
  [string]$NginxRoot = "C:\nginx"
)

$NginxExe = Join-Path $NginxRoot "nginx.exe"
if (Test-Path $NginxExe) {
  Start-Process -FilePath $NginxExe -ArgumentList "-s stop" -WorkingDirectory $NginxRoot -WindowStyle Hidden -Wait
}

Get-Process -Name nginx -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "nginx stopped."
