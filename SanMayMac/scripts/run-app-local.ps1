# Run Spring Boot on host machine (Java 21), DB/Redis/RabbitMQ in Docker
Set-Location $PSScriptRoot\..

if (-not (Test-Path ".env")) {
    Write-Host "Missing .env — copy from .env.example first:" -ForegroundColor Yellow
    exit 1
}

Write-Host "Ensuring infra is up..." -ForegroundColor Cyan
docker compose up -d postgres redis rabbitmq
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Load .env for bootRun (localhost hosts)
Get-Content ".env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $pair = $_ -split '=', 2
    if ($pair.Count -eq 2) {
        Set-Item -Path "Env:$($pair[0].Trim())" -Value $pair[1].Trim()
    }
}

$env:SPRING_PROFILES_ACTIVE = "local"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/sanmaymac"
$env:SPRING_DATASOURCE_USERNAME = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "sanmaymac" }
$env:SPRING_DATASOURCE_PASSWORD = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "sanmaymac" }
$env:SPRING_REDIS_HOST = "localhost"
$env:SPRING_RABBITMQ_HOST = "localhost"

Write-Host "Starting backend (profile=local)..." -ForegroundColor Cyan
.\gradlew bootRun --no-daemon
