[CmdletBinding()]
param([string]$Repository = 'NguynNe3110/Vault-Key')

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$signingDir = Join-Path $projectRoot '.release-signing'
$keystore = Join-Path $signingDir 'vaultkey-release.jks'
$credentialsFile = Join-Path $signingDir 'credentials.json'
Get-Command gh -ErrorAction Stop | Out-Null
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Run gh auth login first, then rerun this script.' }
if (!(Test-Path -LiteralPath $keystore) -or !(Test-Path -LiteralPath $credentialsFile)) {
    throw 'Missing .release-signing/ material. Initialize a new key or restore the original key first.'
}
$credentials = Get-Content -LiteralPath $credentialsFile -Raw | ConvertFrom-Json
$values = @{
    KEYSTORE_BASE64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
    STORE_PASSWORD = $credentials.STORE_PASSWORD
    KEY_ALIAS = $credentials.KEY_ALIAS
    KEY_PASSWORD = $credentials.KEY_PASSWORD
}
$firebaseConfig = Join-Path $projectRoot 'app/google-services.json'
if (Test-Path -LiteralPath $firebaseConfig) {
    $values.GOOGLE_SERVICES_JSON_BASE64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($firebaseConfig))
}
foreach ($name in $values.Keys) {
    if ([string]::IsNullOrWhiteSpace($values[$name])) { throw "Missing secret value: $name" }
}
foreach ($name in $values.Keys) {
    # Feed values through stdin so they never appear in command arguments or logs.
    $values[$name] | & gh secret set $name --repo $Repository
    if ($LASTEXITCODE -ne 0) { throw "Could not set repository secret: $name" }
    Write-Host "Configured $name on $Repository"
}
