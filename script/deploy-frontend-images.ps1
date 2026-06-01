param(
  [Parameter(Mandatory = $true)]
  [string]$Server,

  [string]$RemoteDir = "/opt/code",
  [string]$Platform = "linux/amd64",
  [string]$ClientApiBaseUrl = "",
  [string]$ClientAppApiPrefix = "/app-api",
  [ValidateSet("all", "admin", "client")]
  [string]$Target = "all",
  [string]$ArchiveName = "",
  [string]$ComposeFile = "docker-compose.frontend.yml",
  [switch]$SkipBuild,
  [switch]$SkipSave,
  [switch]$SkipUpload
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$AdminDir = Join-Path $RootDir "yudao-ui\draw2video-admin"
$ClientDir = Join-Path $RootDir "yudao-ui\draw2video-client"

if ([string]::IsNullOrWhiteSpace($ArchiveName)) {
  if ($Target -eq "all") {
    $ArchiveName = "draw2video-frontend.tar"
  } else {
    $ArchiveName = "draw2video-${Target}.tar"
  }
}

$ArchivePath = Join-Path $RootDir $ArchiveName

$Images = @()
$Services = @()
if ($Target -eq "all" -or $Target -eq "admin") {
  $Images += "draw2video-admin:latest"
  $Services += "draw2video-admin"
}
if ($Target -eq "all" -or $Target -eq "client") {
  $Images += "draw2video-client:latest"
  $Services += "draw2video-client"
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

Invoke-Step "Check directories" {
  if (($Target -eq "all" -or $Target -eq "admin") -and !(Test-Path $AdminDir)) { throw "Admin directory not found: $AdminDir" }
  if (($Target -eq "all" -or $Target -eq "client") -and !(Test-Path $ClientDir)) { throw "Client directory not found: $ClientDir" }
}

if (!$SkipBuild) {
  if ($Target -eq "all" -or $Target -eq "admin") {
    Invoke-Step "Build draw2video-admin image" {
      Run-Command "docker" @(
        "buildx", "build",
        "--platform", $Platform,
        "-t", "draw2video-admin:latest",
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
        "-t", "draw2video-client:latest",
        "--load",
        $ClientDir
      )
    }
  }
}

if (!$SkipSave) {
  Invoke-Step "Save frontend images" {
    if (Test-Path $ArchivePath) { Remove-Item $ArchivePath -Force }
    Run-Command "docker" (@("save", "-o", $ArchivePath) + $Images)
  }
}

if (!$SkipUpload) {
  Invoke-Step "Upload image archive" {
    Run-Command "ssh" @($Server, "mkdir -p $RemoteDir")
    Run-Command "scp" @($ArchivePath, "${Server}:${RemoteDir}/${ArchiveName}")
  }

  Invoke-Step "Load images and restart containers" {
    $RemoteCommand = "cd $RemoteDir; docker load -i $ArchiveName; docker compose -f $ComposeFile up -d --force-recreate $($Services -join ' ')"
    Run-Command "ssh" @($Server, $RemoteCommand)
  }
}

Write-Host "`nDone" -ForegroundColor Green
