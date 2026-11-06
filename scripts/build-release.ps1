[CmdletBinding()]
param(
    [ValidatePattern('^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$')]
    [string]$VersionName = '1.0.0',
    [switch]$InitializeSigning
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$signingDir = Join-Path $projectRoot '.release-signing'
$keystore = Join-Path $signingDir 'vaultkey-release.jks'
$credentialsFile = Join-Path $signingDir 'credentials.json'
$parts = $VersionName.Split('.') | ForEach-Object { [long]$_ }
$versionCode = $parts[0] * 1000000 + $parts[1] * 1000 + $parts[2]
if ($parts[0] -gt 2099 -or $parts[1] -gt 999 -or $parts[2] -gt 999 -or $versionCode -lt 1) {
    throw 'Version is outside the supported Android versionCode range.'
}

if ($InitializeSigning) {
    if ((Test-Path -LiteralPath $keystore) -or (Test-Path -LiteralPath $credentialsFile)) {
        throw 'Signing material already exists. Back it up and reuse it; it will not be overwritten.'
    }
    $keytool = (Get-Command keytool -ErrorAction Stop).Source
    New-Item -ItemType Directory -Path $signingDir -Force | Out-Null
    $randomBytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($randomBytes) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($randomBytes)
    $previousPassword = $env:VAULTKEY_KEYTOOL_PASSWORD
    try {
        $env:VAULTKEY_KEYTOOL_PASSWORD = $password
        & $keytool -genkeypair -noprompt -storetype JKS -keystore $keystore `
            -alias upload -keyalg RSA -keysize 4096 -validity 10000 `
            -dname 'CN=VaultKey, OU=Android, O=VaultKey' `
            -storepass:env VAULTKEY_KEYTOOL_PASSWORD -keypass:env VAULTKEY_KEYTOOL_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the release signing key.' }
        @{ STORE_PASSWORD = $password; KEY_ALIAS = 'upload'; KEY_PASSWORD = $password } |
            ConvertTo-Json | Set-Content -LiteralPath $credentialsFile -Encoding UTF8
    } finally {
        $env:VAULTKEY_KEYTOOL_PASSWORD = $previousPassword
    }
    Write-Host 'Created private signing material in .release-signing/. Back up this directory securely.'
}

if (!(Test-Path -LiteralPath $keystore) -or !(Test-Path -LiteralPath $credentialsFile)) {
    throw 'Missing local signing material. For a NEW app, run with -InitializeSigning. For an existing app, reuse its original key.'
}
$credentials = Get-Content -LiteralPath $credentialsFile -Raw | ConvertFrom-Json
foreach ($name in 'STORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace($credentials.$name)) { throw "Missing signing setting: $name" }
}

$sdk = $env:ANDROID_HOME
if (!$sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (!$sdk) {
    $localProperties = Join-Path $projectRoot 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $sdk = $sdkLine.Substring(8).Replace('\:', ':').Replace('\\', '\') }
    }
}
if (!$sdk) { throw 'Configure ANDROID_HOME or sdk.dir in local.properties first.' }
$apksigner = Join-Path $sdk 'build-tools/36.0.0/apksigner.bat'
if (!(Test-Path -LiteralPath $apksigner)) { throw 'Install Android SDK Build-Tools 36.0.0 and configure ANDROID_HOME or local.properties.' }

$previousEnv = @{}
foreach ($name in 'KEYSTORE_PATH', 'STORE_PASSWORD', 'KEY_ALIAS', 'KEY_PASSWORD') {
    $previousEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
Push-Location $projectRoot
try {
    $env:KEYSTORE_PATH = $keystore
    $env:STORE_PASSWORD = $credentials.STORE_PASSWORD
    $env:KEY_ALIAS = $credentials.KEY_ALIAS
    $env:KEY_PASSWORD = $credentials.KEY_PASSWORD
    & .\gradlew.bat :app:assembleRelease "-PversionName=$VersionName" "-PversionCode=$versionCode" --no-configuration-cache --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
    $apk = Join-Path $projectRoot 'app/build/outputs/apk/release/app-release.apk'
    & $apksigner verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    New-Item -ItemType Directory -Path (Join-Path $projectRoot 'dist') -Force | Out-Null
    $output = Join-Path $projectRoot "dist/VaultKey-$VersionName.apk"
    Copy-Item -LiteralPath $apk -Destination $output -Force
    $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  VaultKey-$VersionName.apk" | Set-Content -LiteralPath "$output.sha256" -Encoding ASCII
    Write-Host "Signed APK: $output"
} finally {
    Pop-Location
    foreach ($name in $previousEnv.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousEnv[$name], 'Process')
    }
}
