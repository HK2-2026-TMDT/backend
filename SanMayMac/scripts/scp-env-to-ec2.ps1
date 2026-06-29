# Upload .env to EC2 via SCP (run from backend/SanMayMac)
#
# Quick start:
#   copy ec2.deploy.local.example ec2.deploy.local
#   # edit ec2.deploy.local with EC2 IP + key path
#   .\scripts\scp-env-to-ec2.ps1
#
# Or pass params directly:
#   .\scripts\scp-env-to-ec2.ps1 -Ec2Host ec2-user@1.2.3.4 -KeyPath C:\Users\you\Downloads\tmdt.pem

param(
    [string]$Ec2Host,

    [string]$KeyPath,

    [string]$RemoteDir,

    [string]$LocalEnvFile = ".env",

    [string]$DeployConfigFile = "ec2.deploy.local"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (Test-Path $DeployConfigFile) {
    Get-Content $DeployConfigFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        $pair = $_ -split '=', 2
        if ($pair.Count -ne 2) { return }
        $name = $pair[0].Trim()
        $value = $pair[1].Trim()
        switch ($name) {
            'EC2_HOST' { if (-not $Ec2Host) { $Ec2Host = $value } }
            'KEY_PATH' { if (-not $KeyPath) { $KeyPath = $value } }
            'REMOTE_DIR' { if (-not $RemoteDir) { $RemoteDir = $value } }
        }
    }
}

if (-not $Ec2Host -or $Ec2Host -match 'YOUR_EC2') {
    Write-Host "Missing EC2 host. Set EC2_HOST in ec2.deploy.local or pass -Ec2Host." -ForegroundColor Red
    Write-Host "Example: copy ec2.deploy.local.example ec2.deploy.local"
    exit 1
}

if (-not $KeyPath) {
    $KeyPath = "$env:USERPROFILE\Downloads\tmdt.pem"
}

if (-not $RemoteDir) {
    $RemoteDir = "/home/ec2-user/backend/SanMayMac"
}

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

Write-Host "Done. Restart stack on EC2:" -ForegroundColor Green
Write-Host "  ssh -i `"$KeyPath`" $Ec2Host"
Write-Host "  cd $RemoteDir && docker compose up -d --build"
