param(
  [Parameter(Mandatory = $true)]
  [string]$Server,

  [string]$RemoteDir = "/opt/code",
  [string]$Platform = "linux/amd64",
  [string]$ClientApiBaseUrl = "",
  [string]$ClientAppApiPrefix = "/app-api",
  # [string]$ClientWsBaseUrl = "ws://111.228.39.103:48080",
  [string]$ClientWsBaseUrl = "wss://beta.copse.top",
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

if ([string]::IsNullOrWhiteSpace($ImageTag)) {
  $ImageTag = (git -C $RootDir rev-parse --short=12 HEAD 2>$null)
  if ([string]::IsNullOrWhiteSpace($ImageTag)) {
    $ImageTag = "latest"
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

if (!$SkipBuild) {
  if ($Target -eq "all" -or $Target -eq "admin") {
    Invoke-Step "Build draw2video-admin image" {
      Run-Command "docker" @(
        "buildx", "build",
        "--platform", $Platform,
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
  }

  if ($UseRegistry) {
    Invoke-Step "Pull images and restart containers" {
      $RemoteCommand = "cd $RemoteDir; FRONTEND_IMAGE_TAG=$ImageTag FRONTEND_IMAGE_REGISTRY_PREFIX=${RemoteRegistry}/ docker compose -f $ComposeFile pull $($Services -join ' '); FRONTEND_IMAGE_TAG=$ImageTag FRONTEND_IMAGE_REGISTRY_PREFIX=${RemoteRegistry}/ docker compose -f $ComposeFile up -d --no-build --force-recreate $($Services -join ' ')"
      Run-Command "ssh" @($Server, $RemoteCommand)
    }
  } else {
    Invoke-Step "Upload image archive" {
      Run-Command "scp" @($ArchivePath, "${Server}:${RemoteDir}/${ArchiveName}")
    }

    Invoke-Step "Load images and restart containers" {
      $RemoteCommand = "cd $RemoteDir; docker load -i $ArchiveName; FRONTEND_IMAGE_TAG=$ImageTag docker compose -f $ComposeFile up -d --no-build --force-recreate $($Services -join ' ')"
      Run-Command "ssh" @($Server, $RemoteCommand)
    }
  }
}

Write-Host "`nDone" -ForegroundColor Green
