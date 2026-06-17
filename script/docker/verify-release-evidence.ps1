param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("preflight", "db-evidence", "verify-http", "verify-service-health")]
  [string]$Command,

  [int]$TimeoutSeconds = 120,
  [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$ScriptPath = Join-Path $PSScriptRoot "verify-release-evidence.sh"
if ([string]::IsNullOrWhiteSpace($LogDir)) {
  $stamp = "{0}-{1}" -f (Get-Date -Format "yyyyMMdd-HHmmss-fff"), ([guid]::NewGuid().ToString("N").Substring(0, 8))
  $LogDir = Join-Path $RootDir "tmp\release-gates\windows-verify-$stamp"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$LogPath = Join-Path $LogDir "verify-release-evidence-$Command.log"

function Write-Log {
  param([string]$Message)
  $Message | Tee-Object -FilePath $LogPath -Append | Out-Null
}

function Resolve-BashPath {
  if (![string]::IsNullOrWhiteSpace($env:GIT_BASH_PATH) -and (Test-Path -LiteralPath $env:GIT_BASH_PATH)) {
    return $env:GIT_BASH_PATH
  }

  $candidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files\Git\usr\bin\bash.exe",
    "C:\Program Files (x86)\Git\bin\bash.exe"
  )
  foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate) {
      return $candidate
    }
  }

  $command = Get-Command bash.exe -ErrorAction SilentlyContinue
  if ($null -ne $command) {
    return $command.Source
  }

  return ""
}

Write-Log "START $(Get-Date -Format o)"
Write-Log "command=$Command"
Write-Log "script=$ScriptPath"

$bashPath = Resolve-BashPath
if ([string]::IsNullOrWhiteSpace($bashPath)) {
  Write-Log "execution environment failure: bash executable was not found. Install Git Bash, set GIT_BASH_PATH, or run through WSL."
  Write-Log "END $(Get-Date -Format o) exit=126"
  exit 126
}

Write-Log "bash=$bashPath"

$process = Start-Process `
  -FilePath $bashPath `
  -ArgumentList @($ScriptPath, $Command) `
  -WorkingDirectory $RootDir `
  -NoNewWindow `
  -PassThru `
  -RedirectStandardOutput (Join-Path $LogDir "stdout.log") `
  -RedirectStandardError (Join-Path $LogDir "stderr.log")

if (!$process.WaitForExit($TimeoutSeconds * 1000)) {
  try {
    $process.Kill()
  } catch {
    Write-Log "execution environment failure: failed to stop timed out bash process: $($_.Exception.Message)"
  }
  Write-Log "execution environment failure: bash startup or script execution exceeded ${TimeoutSeconds}s"
  Write-Log "END $(Get-Date -Format o) exit=124"
  exit 124
}

$stdout = Join-Path $LogDir "stdout.log"
$stderr = Join-Path $LogDir "stderr.log"
if (Test-Path -LiteralPath $stdout) {
  Get-Content -LiteralPath $stdout | Add-Content -Path $LogPath
}
if (Test-Path -LiteralPath $stderr) {
  Get-Content -LiteralPath $stderr | Add-Content -Path $LogPath
}

$exitCode = $process.ExitCode
if ($null -eq $exitCode) {
  Write-Log "execution environment failure: bash process did not return an exit code"
  Write-Log "END $(Get-Date -Format o) exit=126"
  exit 126
}

if ($exitCode -ne 0) {
  Write-Log "release evidence gate failed: verify-release-evidence.sh returned $exitCode"
} else {
  Write-Log "release evidence gate passed"
}
Write-Log "END $(Get-Date -Format o) exit=$exitCode"
exit $exitCode
