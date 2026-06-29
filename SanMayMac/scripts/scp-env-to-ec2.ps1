# Upload .env to EC2 via SCP (run from backend/SanMayMac)
#
# Examples:
#   .\scripts\scp-env-to-ec2.ps1 -Ec2Host ec2-user@3.110.xxx.xxx -KeyPath C:\Users\you\Downloads\sanmaymac.pem
#   .\scripts\scp-env-to-ec2.ps1 -Ec2Host ubuntu@3.110.xxx.xxx -KeyPath ~/.ssh/key.pem -RemoteDir /opt/sanmaymac

param(
    [Parameter(Mandatory = $true)]
    [string]$Ec2Host,

    [Parameter(Mandatory = $true)]
    [string]$KeyPath,

    [string]$RemoteDir = "/home/ec2-user/sanmaymac/SanMayMac",

    [string]$LocalEnvFile = ".env"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (-not (Test-Path $LocalEnvFile)) {
    Write-Host "Missing $LocalEnvFile — copy from .env.example first:" -ForegroundColor Red
    Write-Host "  copy .env.example .env"
    exit 1
}

if (-not (Test-Path $KeyPath)) {
    Write-Host "SSH key not found: $KeyPath" -ForegroundColor Red
    exit 1
}

$key = Resolve-Path $KeyPath
$remotePath = "$RemoteDir/.env"

Write-Host "Creating remote directory (if needed)..." -ForegroundColor Cyan
ssh -i $key -o StrictHostKeyChecking=accept-new $Ec2Host "mkdir -p $RemoteDir"

Write-Host "Uploading $LocalEnvFile -> ${Ec2Host}:${remotePath}" -ForegroundColor Cyan
scp -i $key $LocalEnvFile "${Ec2Host}:${remotePath}"

if ($LASTEXITCODE -ne 0) {
    Write-Host "SCP failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Done. On EC2, verify and restart:" -ForegroundColor Green
Write-Host "  ssh -i `"$KeyPath`" $Ec2Host"
Write-Host "  cd $RemoteDir && docker compose up -d --build"
