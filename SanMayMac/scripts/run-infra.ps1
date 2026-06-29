# Start only infrastructure: PostgreSQL + Redis + RabbitMQ
# Use with ./gradlew bootRun and profile "local"
Set-Location $PSScriptRoot\..

if (-not (Test-Path ".env")) {
    Write-Host "Missing .env — copy from .env.example first:" -ForegroundColor Yellow
    Write-Host "  copy .env.example .env"
    exit 1
}

Write-Host "Starting infra (postgres, redis, rabbitmq)..." -ForegroundColor Cyan
docker compose up -d postgres redis rabbitmq

Write-Host ""
Write-Host "Infra ready. Connect app with:" -ForegroundColor Green
Write-Host "  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sanmaymac"
Write-Host "  .\gradlew bootRun -Dspring.profiles.active=local"
Write-Host ""
Write-Host "Or run: .\scripts\run-app-local.ps1"
