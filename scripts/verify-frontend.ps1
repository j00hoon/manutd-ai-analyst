
$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\frontend"

Write-Host "Running frontend verification..."

npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Frontend build failed"
}

Write-Host "Frontend verification passed"
