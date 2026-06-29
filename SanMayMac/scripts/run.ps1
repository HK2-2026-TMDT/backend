# Build and run full stack: PostgreSQL + Redis + RabbitMQ + Backend
Set-Location $PSScriptRoot\..

if (-not (Test-Path ".env")) {
    Write-Host "Missing .env — copy from .env.example first:" -ForegroundColor Yellow
    Write-Host "  copy .env.example .env"
    exit 1
}

Write-Host "Starting SanMayMac (PostgreSQL local)..." -ForegroundColor Cyan
docker compose up --build
