# Build all Docker images (backend + dependencies)
Set-Location $PSScriptRoot\..

Write-Host "Building SanMayMac Docker images..." -ForegroundColor Cyan
docker compose build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Build complete." -ForegroundColor Green
