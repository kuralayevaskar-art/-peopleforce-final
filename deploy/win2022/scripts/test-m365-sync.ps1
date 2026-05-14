$ErrorActionPreference = "Stop"

Write-Host "You will be asked for the account that can run Start-ADSyncSyncCycle on 10.1.10.11."
$cred = Get-Credential
Invoke-Command -ComputerName 10.1.10.11 -Credential $cred -ScriptBlock {
  Import-Module ADSync
  Start-ADSyncSyncCycle -PolicyType Delta
}
