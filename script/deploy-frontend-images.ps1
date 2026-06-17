param(
  [Parameter(Mandatory = $true)]
  [string]$Server,

  [string]$RemoteDir = "/opt/code",
  [string]$Platform = "linux/amd64",
  [ValidateSet("auto", "test", "prod")]
  [string]$DeployEnv = "auto",
  [string]$AdminBuildMode = "",
  [string]$ClientApiBaseUrl = "",
  [string]$ClientAppApiPrefix = "/app-api",
  # [string]$ClientWsBaseUrl = "ws://111.228.39.103:48080",
  [string]$ClientWsBaseUrl = "",
  [string]$ClientGatewayHost = "host.docker.internal",
  [string]$ClientGatewayPort = "48080",
  [string]$ClientTenantId = "1",
  [string]$ClientTerminal = "20",
  [string]$AdminGatewayHost = "host.docker.internal",
  [string]$AdminGatewayPort = "48080",
  [ValidateSet("all", "admin", "client", "guide")]
  [string]$Target = "all",
  [string]$ImageTag = "",
  [string]$ArchiveName = "",
  [string]$ComposeFile = "docker-compose.frontend.yml",
  [switch]$UseRegistry,
  [string]$Registry = "111.228.39.103:3000/root",
  [string]$RemoteRegistry = "",
  [switch]$SkipBuild,
  [switch]$SkipSave,
  [switch]$SkipUpload
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$AdminDir = Join-Path $RootDir "yudao-ui\draw2video-admin"
$ClientDir = Join-Path $RootDir "yudao-ui\draw2video-client"
$GuideDir = Join-Path $RootDir "yudao-ui\draw2video-guide"
$ComposeSourcePath = Join-Path $RootDir "script\docker\$ComposeFile"
$TestImageVersionFile = Join-Path $RootDir "script\docker\test-image-version"

function Get-TestImageVersion {
  if (!(Test-Path -LiteralPath $TestImageVersionFile)) {
    throw "Test image version file not found: $TestImageVersionFile"
  }

  $Version = (Get-Content -LiteralPath $TestImageVersionFile -Raw).Trim()
  if ($Version -notmatch '^v[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.-]+)?$') {
    throw "Invalid test image version in ${TestImageVersionFile}: $Version"
  }
  return $Version
}

if ($DeployEnv -eq "auto") {
  if ($Server -eq "manman2" -or $Server -eq "root@117.72.215.47") {
    $DeployEnv = "prod"
  } else {
    $DeployEnv = "test"
  }
}

if ([string]::IsNullOrWhiteSpace($AdminBuildMode)) {
  $AdminBuildMode = $DeployEnv
}

if ([string]::IsNullOrWhiteSpace($ClientWsBaseUrl)) {
  $ClientWsBaseUrl = if ($DeployEnv -eq "prod") { "wss://beta.copse.top" } else { "" }
}

if ([string]::IsNullOrWhiteSpace($ImageTag)) {
  if ($DeployEnv -eq "test") {
    $ImageTag = Get-TestImageVersion
  } else {
    $GitTag = (git -C $RootDir rev-parse --short=12 HEAD 2>$null)
    if ([string]::IsNullOrWhiteSpace($GitTag)) {
      $GitTag = "latest"
    }
    $ImageTag = "${DeployEnv}-${GitTag}"
  }
}

if ([string]::IsNullOrWhiteSpace($ArchiveName)) {
  if ($Target -eq "all") {
    $ArchiveName = "draw2video-frontend.tar"
  } else {
    $ArchiveName = "draw2video-${Target}.tar"
  }
}

if ([string]::IsNullOrWhiteSpace($RemoteRegistry)) {
  if ($Server -eq "manman" -or $Server -eq "root@111.228.39.103") {
    $RemoteRegistry = "127.0.0.1:3000/root"
  } else {
    $RemoteRegistry = $Registry
  }
}

$ArchivePath = Join-Path $RootDir $ArchiveName

$Images = @()
$RegistryImages = @()
$Services = @()
if ($Target -eq "all" -or $Target -eq "admin") {
  $Images += "draw2video-admin:$ImageTag"
  $RegistryImages += "${Registry}/draw2video-admin:$ImageTag"
  $Services += "draw2video-admin"
}
if ($Target -eq "all" -or $Target -eq "client") {
  $Images += "draw2video-client:$ImageTag"
  $RegistryImages += "${Registry}/draw2video-client:$ImageTag"
  $Services += "draw2video-client"
}
if ($Target -eq "all" -or $Target -eq "guide") {
  $Images += "draw2video-guide:$ImageTag"
  $RegistryImages += "${Registry}/draw2video-guide:$ImageTag"
  $Services += "draw2video-guide"
}

function Invoke-Step {
  param(
    [string]$Title,
    [scriptblock]$Action
  )
  Write-Host "`n==> $Title" -ForegroundColor Cyan
  & $Action
}

function Run-Command {
  param(
    [string]$FilePath,
    [string[]]$Arguments
  )
  Write-Host "> $FilePath $($Arguments -join ' ')" -ForegroundColor DarkGray
  & $FilePath @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
  }
}

function Remove-PathWithRetry {
  param(
    [string]$Path,
    [int]$Attempts = 3
  )

  for ($Attempt = 1; $Attempt -le $Attempts; $Attempt++) {
    if (!(Test-Path -LiteralPath $Path)) {
      return
    }

    try {
      Remove-Item -LiteralPath $Path -Force -ErrorAction Stop
      return
    } catch {
      if ($Attempt -eq $Attempts) {
        throw
      }
      Start-Sleep -Seconds $Attempt
    }
  }
}

function Save-DockerImages {
  param(
    [string]$OutputPath,
    [string[]]$ImageNames
  )

  $OutputDir = Split-Path -Parent $OutputPath
  $OutputName = Split-Path -Leaf $OutputPath
  $Attempts = 3

  for ($Attempt = 1; $Attempt -le $Attempts; $Attempt++) {
    $TempPath = Join-Path $OutputDir ".${OutputName}.save-${PID}-${Attempt}.tmp"
    try {
      Remove-PathWithRetry $TempPath
      Run-Command "docker" (@("save", "-o", $TempPath) + $ImageNames)
      Remove-PathWithRetry $OutputPath
      Move-Item -LiteralPath $TempPath -Destination $OutputPath -Force -ErrorAction Stop
      return
    } catch {
      Remove-PathWithRetry $TempPath
      if ($Attempt -eq $Attempts) {
        throw
      }
      Write-Warning "Save archive failed (attempt ${Attempt}/${Attempts}): $($_.Exception.Message). Retrying..."
      Start-Sleep -Seconds ([Math]::Min(5, $Attempt * 2))
    }
  }
}

Invoke-Step "Check directories" {
  if (($Target -eq "all" -or $Target -eq "admin") -and !(Test-Path $AdminDir)) { throw "Admin directory not found: $AdminDir" }
  if (($Target -eq "all" -or $Target -eq "client") -and !(Test-Path $ClientDir)) { throw "Client directory not found: $ClientDir" }
  if (($Target -eq "all" -or $Target -eq "guide") -and !(Test-Path $GuideDir)) { throw "Guide directory not found: $GuideDir" }
}

function ConvertTo-EnvValue {
  param([string]$Value)

  if ($null -eq $Value) {
    return ""
  }
  return ($Value -replace '\\', '\\' -replace "`r", "" -replace "`n", "")
}

function New-FrontendEnvFile {
  $envFile = Join-Path ([System.IO.Path]::GetTempPath()) "frontend-${DeployEnv}-${PID}.env"
  $AdminPort = if ($DeployEnv -eq "prod") { "8081" } else { "8081" }
  $ClientPort = if ($DeployEnv -eq "prod") { "13000" } else { "13000" }
  $GuidePort = if ($DeployEnv -eq "prod") { "8082" } else { "8082" }
  $lines = @(
    "FRONTEND_DEPLOY_ENV=$(ConvertTo-EnvValue $DeployEnv)",
    "FRONTEND_IMAGE_TAG=$(ConvertTo-EnvValue $ImageTag)",
    "FRONTEND_IMAGE_REGISTRY_PREFIX=$(ConvertTo-EnvValue "${RemoteRegistry}/")",
    "DRAW2VIDEO_ADMIN_PORT=$(ConvertTo-EnvValue $AdminPort)",
    "DRAW2VIDEO_CLIENT_PORT=$(ConvertTo-EnvValue $ClientPort)",
    "DRAW2VIDEO_GUIDE_PORT=$(ConvertTo-EnvValue $GuidePort)",
    "ADMIN_GATEWAY_HOST=$(ConvertTo-EnvValue $AdminGatewayHost)",
    "ADMIN_GATEWAY_PORT=$(ConvertTo-EnvValue $AdminGatewayPort)",
    "CLIENT_GATEWAY_HOST=$(ConvertTo-EnvValue $ClientGatewayHost)",
    "CLIENT_GATEWAY_PORT=$(ConvertTo-EnvValue $ClientGatewayPort)",
    "CLIENT_API_BASE_URL=$(ConvertTo-EnvValue $ClientApiBaseUrl)",
    "CLIENT_APP_API_PREFIX=$(ConvertTo-EnvValue $ClientAppApiPrefix)",
    "CLIENT_WS_BASE_URL=$(ConvertTo-EnvValue $ClientWsBaseUrl)",
    "CLIENT_TENANT_ID=$(ConvertTo-EnvValue $ClientTenantId)",
    "CLIENT_TERMINAL=$(ConvertTo-EnvValue $ClientTerminal)"
  )
  [System.IO.File]::WriteAllText($envFile, ($lines -join [Environment]::NewLine) + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
  return $envFile
}

if (!$SkipBuild) {
  if ($Target -eq "all" -or $Target -eq "admin") {
    Invoke-Step "Build draw2video-admin image" {
      Run-Command "docker" @(
        "buildx", "build",
        "--platform", $Platform,
        "--build-arg", "ADMIN_BUILD_MODE=$AdminBuildMode",
        "-t", "draw2video-admin:$ImageTag",
        "--load",
        $AdminDir
      )
    }
  }

  if ($Target -eq "all" -or $Target -eq "client") {
    Invoke-Step "Build draw2video-client image" {
      Run-Command "docker" @(
        "buildx", "build",
        "--platform", $Platform,
        "--build-arg", "NEXT_PUBLIC_API_BASE_URL=$ClientApiBaseUrl",
        "--build-arg", "NEXT_PUBLIC_APP_API_PREFIX=$ClientAppApiPrefix",
        "--build-arg", "NEXT_PUBLIC_WS_BASE_URL=$ClientWsBaseUrl",
        "--build-arg", "NEXT_PUBLIC_TENANT_ID=$ClientTenantId",
        "--build-arg", "NEXT_PUBLIC_TERMINAL=$ClientTerminal",
        "-t", "draw2video-client:$ImageTag",
        "--load",
        $ClientDir
      )
    }
  }

  if ($Target -eq "all" -or $Target -eq "guide") {
    Invoke-Step "Build draw2video-guide image" {
      Run-Command "docker" @(
        "buildx", "build",
        "--platform", $Platform,
        "-t", "draw2video-guide:$ImageTag",
        "--load",
        $GuideDir
      )
    }
  }
}

if ($UseRegistry) {
  Invoke-Step "Push frontend images to registry" {
    for ($i = 0; $i -lt $Images.Count; $i++) {
      Run-Command "docker" @("tag", $Images[$i], $RegistryImages[$i])
      Run-Command "docker" @("push", $RegistryImages[$i])
    }
  }
} elseif (!$SkipSave) {
  Invoke-Step "Save frontend images" {
    Save-DockerImages $ArchivePath $Images
  }
}

if (!$SkipUpload) {
  Invoke-Step "Prepare remote compose file" {
    Run-Command "ssh" @($Server, "mkdir -p $RemoteDir")
    if (Test-Path $ComposeSourcePath) {
      Run-Command "scp" @($ComposeSourcePath, "${Server}:${RemoteDir}/${ComposeFile}")
    } else {
      Write-Warning "Compose file not found locally: $ComposeSourcePath. Remote compose file will be reused."
    }
    $LocalEnvFile = New-FrontendEnvFile
    try {
      Run-Command "scp" @($LocalEnvFile, "${Server}:${RemoteDir}/.frontend-${DeployEnv}.env")
    } finally {
      Remove-PathWithRetry $LocalEnvFile
    }
  }

  if ($UseRegistry) {
    Invoke-Step "Pull images and restart containers" {
      $RemoteCommand = "cd $RemoteDir; FRONTEND_IMAGE_TAG=$ImageTag FRONTEND_IMAGE_REGISTRY_PREFIX=${RemoteRegistry}/ docker compose --env-file .frontend-${DeployEnv}.env -f $ComposeFile pull $($Services -join ' '); FRONTEND_IMAGE_TAG=$ImageTag FRONTEND_IMAGE_REGISTRY_PREFIX=${RemoteRegistry}/ docker compose --env-file .frontend-${DeployEnv}.env -f $ComposeFile up -d --no-build --force-recreate $($Services -join ' ')"
      Run-Command "ssh" @($Server, $RemoteCommand)
    }
  } else {
    Invoke-Step "Upload image archive" {
      Run-Command "scp" @($ArchivePath, "${Server}:${RemoteDir}/${ArchiveName}")
    }

    Invoke-Step "Load images and restart containers" {
      $RemoteCommand = "cd $RemoteDir; docker load -i $ArchiveName; FRONTEND_IMAGE_TAG=$ImageTag docker compose --env-file .frontend-${DeployEnv}.env -f $ComposeFile up -d --no-build --force-recreate $($Services -join ' ')"
      Run-Command "ssh" @($Server, $RemoteCommand)
    }
  }
}

Write-Host "`nDone" -ForegroundColor Green
