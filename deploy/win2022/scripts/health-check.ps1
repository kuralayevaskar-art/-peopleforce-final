$ErrorActionPreference = "Continue"

Write-Host "Checking backend..."
try {
  Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/health" -TimeoutSec 10 | ConvertTo-Json -Depth 5
} catch {
  Write-Host "Backend health failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Checking frontend..."
try {
  $response = Invoke-WebRequest -Uri "http://127.0.0.1/" -UseBasicParsing -TimeoutSec 10
  Write-Host "Frontend status: $($response.StatusCode)"
} catch {
  Write-Host "Frontend check failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Checking AD module..."
try {
  Get-Command New-ADUser -ErrorAction Stop | Select-Object Name,Source
} catch {
  Write-Host "New-ADUser not available. Install RSAT Active Directory tools." -ForegroundColor Red
}
