$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$verifyDir = Join-Path $tempRoot "ranke-health-test-$PID"
$gradleHome = Join-Path $env:USERPROFILE '.gradle'

if (-not $verifyDir.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Unexpected verification directory: $verifyDir"
}

try {
    New-Item -ItemType Directory -Path $verifyDir -Force | Out-Null

    @(
        'settings.gradle.kts',
        'build.gradle.kts',
        'gradle.properties',
        'gradlew',
        'gradlew.bat'
    ) | ForEach-Object {
        Copy-Item -LiteralPath (Join-Path $projectDir $_) -Destination $verifyDir -Force
    }

    Copy-Item -LiteralPath (Join-Path $projectDir 'gradle') -Destination $verifyDir -Recurse -Force
    New-Item -ItemType Directory -Path (Join-Path $verifyDir 'app') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectDir 'app\build.gradle.kts') -Destination (Join-Path $verifyDir 'app') -Force
    Copy-Item -LiteralPath (Join-Path $projectDir 'app\src') -Destination (Join-Path $verifyDir 'app') -Recurse -Force

    $localProperties = Join-Path $projectDir 'local.properties'
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        Copy-Item -LiteralPath $localProperties -Destination $verifyDir -Force
    } elseif ($env:ANDROID_HOME) {
        $escapedSdk = $env:ANDROID_HOME.Replace('\', '\\').Replace(':', '\:')
        Set-Content -LiteralPath (Join-Path $verifyDir 'local.properties') -Value "sdk.dir=$escapedSdk" -Encoding ascii
    } else {
        throw 'Android SDK path not found. Add local.properties or set ANDROID_HOME.'
    }

    $env:GRADLE_USER_HOME = $gradleHome
    & (Join-Path $verifyDir 'gradlew.bat') -p $verifyDir testDebugUnitTest --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    if ((Test-Path -LiteralPath $verifyDir) -and $verifyDir.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $verifyDir -Recurse -Force
    }
}
