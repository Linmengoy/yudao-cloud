param(
  [string[]]$AllowedIssues = @("#146", "#173", "#174"),
  [string[]]$IncludedIssues = @(),
  [string[]]$ProcessingIssues = @(),
  [string[]]$AllowedFrontendTargets = @("admin"),
  [string]$FrontendTarget = "",
  [string[]]$BackendServices = @("aigc-model", "aigc-gen"),
  [string]$ManifestPath = "",
  [string]$ReleaseNotesPath = ""
)

$ErrorActionPreference = "Stop"

function Normalize-Issue {
  param([string]$Issue)
  $value = $Issue.Trim()
  if ([string]::IsNullOrWhiteSpace($value)) {
    return ""
  }
  if ($value.StartsWith("#")) {
    return $value
  }
  return "#$value"
}

function Expand-Values {
  param([string[]]$Values)
  foreach ($value in $Values) {
    foreach ($item in ($value -split ",")) {
      $trimmed = $item.Trim()
      if (![string]::IsNullOrWhiteSpace($trimmed)) {
        Write-Output $trimmed
      }
    }
  }
}

function Fail-Scope {
  param([string]$Message)
  Write-Error "release scope audit failed: $Message"
  exit 1
}

$allowed = @{}
foreach ($issue in (Expand-Values $AllowedIssues)) {
  $normalized = Normalize-Issue $issue
  if ($normalized) {
    $allowed[$normalized] = $true
  }
}

$processing = @{}
foreach ($issue in (Expand-Values $ProcessingIssues)) {
  $normalized = Normalize-Issue $issue
  if ($normalized) {
    $processing[$normalized] = $true
  }
}

foreach ($issue in (Expand-Values $IncludedIssues)) {
  $normalized = Normalize-Issue $issue
  if (!$normalized) {
    continue
  }
  if ($processing.ContainsKey($normalized)) {
    Fail-Scope "processing issue entered release candidate: $normalized"
  }
  if (!$allowed.ContainsKey($normalized)) {
    Fail-Scope "issue is outside release scope: $normalized"
  }
}

if (![string]::IsNullOrWhiteSpace($FrontendTarget)) {
  if ($AllowedFrontendTargets -notcontains $FrontendTarget) {
    Fail-Scope "frontend target '$FrontendTarget' is outside release scope; allowed targets: $($AllowedFrontendTargets -join ', ')"
  }
}

if (![string]::IsNullOrWhiteSpace($ManifestPath)) {
  if (!(Test-Path -LiteralPath $ManifestPath)) {
    Fail-Scope "manifest not found: $ManifestPath"
  }

  $lineNumber = 0
  foreach ($line in Get-Content -LiteralPath $ManifestPath) {
    $lineNumber++
    $trimmed = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
      continue
    }
    $parts = $trimmed -split "\|"
    if ($parts.Count -lt 3) {
      Fail-Scope "manifest line $lineNumber must be file|issue|status"
    }
    $issue = Normalize-Issue $parts[1]
    $status = $parts[2].Trim()
    if ($processing.ContainsKey($issue) -or $status -eq "review:processing") {
      Fail-Scope "manifest line $lineNumber maps a candidate file to processing work: $issue"
    }
    if (!$allowed.ContainsKey($issue) -and $status -ne "completed-dependency") {
      Fail-Scope "manifest line $lineNumber maps a candidate file outside allowed scope: $issue"
    }
  }
}

if (![string]::IsNullOrWhiteSpace($ReleaseNotesPath)) {
  if (!(Test-Path -LiteralPath $ReleaseNotesPath)) {
    Fail-Scope "release notes not found: $ReleaseNotesPath"
  }
  $notes = Get-Content -LiteralPath $ReleaseNotesPath -Raw
  foreach ($required in @("included issues", "excluded processing issues", "exclusion rationale")) {
    if ($notes -notmatch [regex]::Escape($required)) {
      Fail-Scope "release notes missing scope section field: $required"
    }
  }
}

Write-Output "release scope audit passed"
Write-Output "included issues: $((@(Expand-Values $IncludedIssues) | ForEach-Object { Normalize-Issue $_ }) -join ', ')"
Write-Output "excluded processing issues: $((@(Expand-Values $ProcessingIssues) | ForEach-Object { Normalize-Issue $_ }) -join ', ')"
Write-Output "allowed frontend targets: $($AllowedFrontendTargets -join ', ')"
Write-Output "backend services: $($BackendServices -join ', ')"
