<#
.SYNOPSIS
    把 bao/ 目录下的 APK 上传到 GitHub Release，作为版本归档与下载入口。

.DESCRIPTION
    流程：先执行 .\package-apk.ps1 生成本地 APK，再执行本脚本上传。
    - 版本号取自 app/build.gradle.kts 的 versionName
    - 仓库地址取自 git remote origin
    - Token 优先读环境变量 GITHUB_TOKEN，否则从 Git 凭据管理器读取
    - Release 已存在则复用；同名资产会先删除再上传，可重复执行

.EXAMPLE
    .\publish-release.ps1
#>
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildFile = Join-Path $projectDir 'app\build.gradle.kts'

# ---------- 1. 读取版本号 ----------
$buildConfig = Get-Content -LiteralPath $buildFile -Raw
$versionMatch = [regex]::Match($buildConfig, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) {
    throw "在 $buildFile 中找不到 versionName"
}
$versionName = $versionMatch.Groups[1].Value

# ---------- 2. 定位 APK ----------
$apkPath = Join-Path $projectDir "bao\man-niu-v$versionName.apk"
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) {
    throw "APK 不存在：$apkPath`n请先执行 .\package-apk.ps1 生成安装包"
}
$assetName = Split-Path -Leaf $apkPath
$apkSizeMb = [math]::Round((Get-Item -LiteralPath $apkPath).Length / 1MB, 1)

# ---------- 3. 解析 owner / repo ----------
Push-Location $projectDir
try {
    $remoteUrl = (git remote get-url origin).Trim()
} finally {
    Pop-Location
}
$repoMatch = [regex]::Match($remoteUrl, 'github\.com[:/](?<owner>[^/]+)/(?<repo>[^/.]+)')
if (-not $repoMatch.Success) {
    throw "无法从 origin 解析 GitHub 仓库地址：$remoteUrl"
}
$owner = $repoMatch.Groups['owner'].Value
$repo = $repoMatch.Groups['repo'].Value

# ---------- 4. 获取 Token ----------
$token = $env:GITHUB_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    $cred = "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null
    foreach ($line in $cred) {
        if ($line -like 'password=*') {
            $token = $line.Substring('password='.Length)
        }
    }
}
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "未找到 GitHub 凭据。请设置环境变量 GITHUB_TOKEN，或先用 GitHub Desktop 登录一次"
}

$headers = @{
    Authorization = "token $token"
    Accept        = 'application/vnd.github+json'
    'User-Agent'  = 'manniu-release'
}
$apiBase = "https://api.github.com/repos/$owner/$repo"
$tag = "v$versionName"

Write-Host "仓库 : $owner/$repo"
Write-Host "版本 : $tag"
Write-Host "文件 : $assetName ($apkSizeMb MB)"
Write-Host ""

# ---------- 5. 查找或创建 Release ----------
$release = $null
try {
    $release = Invoke-RestMethod -Uri "$apiBase/releases/tags/$tag" -Headers $headers -Method Get
    Write-Host "复用已有 Release $tag"
} catch {
    Write-Host "创建新 Release $tag"
    $payload = @{
        tag_name   = $tag
        name       = "man牛 $tag"
        body       = "Android 安装包 $tag"
        draft      = $false
        prerelease = $false
    } | ConvertTo-Json
    $release = Invoke-RestMethod -Uri "$apiBase/releases" -Headers $headers -Method Post `
        -Body $payload -ContentType 'application/json'
}

# ---------- 6. 清理同名旧资产 ----------
foreach ($asset in $release.assets) {
    if ($asset.name -eq $assetName) {
        Invoke-RestMethod -Uri "$apiBase/releases/$($release.id)/assets/$($asset.id)" -Headers $headers -Method Delete
        Write-Host "已删除同名旧资产，准备重新上传"
    }
}

# ---------- 7. 上传 ----------
$uploadUrl = "https://uploads.github.com/repos/$owner/$repo/releases/$($release.id)/assets?name=$assetName"
$result = Invoke-RestMethod -Uri $uploadUrl -Headers $headers -Method Post `
    -InFile $apkPath -ContentType 'application/vnd.android.package-archive'

Write-Host ""
Write-Host "上传完成"
Write-Host "下载地址：$($result.browser_download_url)"
