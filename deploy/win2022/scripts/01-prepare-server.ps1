param(
  [string]$AppRoot = "C:\OscarHR"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $AppRoot | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\backend" | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\frontend" | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\config" | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\logs" | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\storage" | Out-Null
New-Item -ItemType Directory -Force -Path "$AppRoot\nginx" | Out-Null

if (-not (Get-NetFirewallRule -DisplayName "OscarHR HTTP 80" -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName "OscarHR HTTP 80" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow | Out-Null
}

if (-not (Get-NetFirewallRule -DisplayName "OscarHR Backend 8080" -ErrorAction SilentlyContinue)) {
  New-NetFirewallRule -DisplayName "OscarHR Backend 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow | Out-Null
}

Write-Host "Installing RSAT Active Directory tools if missing..."
$capability = Get-WindowsCapability -Name RSAT.ActiveDirectory* -Online | Select-Object -First 1
if ($capability -and $capability.State -ne "Installed") {
  Add-WindowsCapability -Online -Name Rsat.ActiveDirectory.DS-LDS.Tools~~~~0.0.1.0 | Out-Null
}

Write-Host "Preparing WinRM TrustedHosts for M365 ADSync server 10.1.10.11..."
Set-Item WSMan:\localhost\Client\TrustedHosts -Value "10.1.10.11" -Force

Write-Host "Done. App root prepared: $AppRoot"
