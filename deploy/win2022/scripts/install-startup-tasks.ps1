param(
  [string]$AppRoot = "C:\OscarHR",
  [string]$NginxRoot = "C:\nginx"
)

$ErrorActionPreference = "Stop"

$backendScript = Join-Path $AppRoot "scripts\start-backend.ps1"
$nginxScript = Join-Path $AppRoot "scripts\start-nginx.ps1"

$backendAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$backendScript`" -AppRoot `"$AppRoot`""
$nginxAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$nginxScript`" -AppRoot `"$AppRoot`" -NginxRoot `"$NginxRoot`""
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest

Register-ScheduledTask -TaskName "OscarHR Backend" -Action $backendAction -Trigger $trigger -Principal $principal -Force | Out-Null
Register-ScheduledTask -TaskName "OscarHR Nginx" -Action $nginxAction -Trigger $trigger -Principal $principal -Force | Out-Null

Write-Host "Startup tasks installed:"
Write-Host "- OscarHR Backend"
Write-Host "- OscarHR Nginx"
