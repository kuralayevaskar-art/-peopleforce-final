param(
  [string]$AppRoot = "C:\OscarHR",
  [string]$NginxRoot = "C:\nginx"
)

& "$PSScriptRoot\stop-backend.ps1" -AppRoot $AppRoot
& "$PSScriptRoot\stop-nginx.ps1" -NginxRoot $NginxRoot
Start-Sleep -Seconds 3
& "$PSScriptRoot\start-backend.ps1" -AppRoot $AppRoot
Start-Sleep -Seconds 8
& "$PSScriptRoot\start-nginx.ps1" -AppRoot $AppRoot -NginxRoot $NginxRoot
& "$PSScriptRoot\health-check.ps1"
