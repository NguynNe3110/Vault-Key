[CmdletBinding()]
param(
    [ValidatePattern('^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$')]
    [string]$VersionName = '1.0.0'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$parts = $VersionName.Split('.') | ForEach-Object { [long]$_ }
$versionCode = $parts[0] * 1000000 + $parts[1] * 1000 + $parts[2]
if ($parts[0] -gt 2099 -or $parts[1] -gt 999 -or $parts[2] -gt 999 -or $versionCode -lt 1) {
    throw 'Version is outside the supported Android versionCode range.'
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

Push-Location $projectRoot
try {
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
}
