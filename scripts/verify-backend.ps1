$ErrorActionPreference = "Stop"

Set-Location "$PSScriptRoot\..\backend"

.\mvnw.cmd clean test

if ($LASTEXITCODE -ne 0) {
    throw "Backend tests failed"
}

Write-Host "Backend verification passed"