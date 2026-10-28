param([string]$SourceApkPath)

$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildFile = Join-Path $projectDir 'app\build.gradle.kts'
$sourceApk = if ($SourceApkPath) {
    [IO.Path]::GetFullPath($SourceApkPath)
} else {
    Join-Path $projectDir 'app\build\outputs\apk\release\app-release.apk'
}
$outputDir = Join-Path $projectDir 'bao'

$buildConfig = Get-Content -LiteralPath $buildFile -Raw
$versionMatch = [regex]::Match($buildConfig, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) {
    throw "Cannot find versionName in $buildFile"
}

if (-not (Test-Path -LiteralPath $sourceApk -PathType Leaf)) {
    throw "APK not found: $sourceApk. Run .\gradlew.bat assembleRelease first."
}

$versionName = $versionMatch.Groups[1].Value
$destinationApk = Join-Path $outputDir "man-niu-v$versionName.apk"
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
Copy-Item -LiteralPath $sourceApk -Destination $destinationApk -Force

Get-Item -LiteralPath $destinationApk | Select-Object FullName, Length, LastWriteTime
