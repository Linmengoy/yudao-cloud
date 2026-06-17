param(
  [string[]]$CandidateIssues = @("#146", "#173", "#174"),
  [string[]]$BuildInputPaths = @(
    ".gitea/workflows/yudao-micro-cicd.yml",
    ".gitea/workflows/yudao-micro-cicd-prod.yml",
    "script/deploy-frontend-images.ps1",
    "script/deploy-frontend-images.sh",
    "script/docker/docker-compose.frontend.yml",
    "script/docker/docker-compose-micro.yml",
    "script/docker/verify-release-evidence.sh",
    "script/docker/verify-release-evidence.ps1",
    "script/release-scope-audit.ps1",
    "yudao-ui/draw2video-admin/src/locales/en.ts",
    "yudao-ui/draw2video-admin/src/locales/zh-CN.ts",
    "yudao-ui/draw2video-admin/src/views/aigc/model/channel/ChannelForm.vue",
    "yudao-ui/draw2video-admin/src/views/aigc/model/channel/index.vue",
    "yudao-ui/draw2video-admin/src/views/aigc/model/model/ModelForm.vue",
    "yudao-ui/draw2video-admin/src/views/aigc/model/provider/ProviderForm.vue"
  ),
  [string]$EvidencePath = ""
)

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$FullSha = (git -C $RootDir rev-parse HEAD).Trim()
$ShortSha = (git -C $RootDir rev-parse --short=12 HEAD).Trim()

if ([string]::IsNullOrWhiteSpace($EvidencePath)) {
  $EvidencePath = Join-Path $RootDir "tmp\release-evidence\release-candidate-$ShortSha.md"
}

$normalizedInputs = @()
foreach ($path in $BuildInputPaths) {
  $trimmed = $path.Trim()
  if (![string]::IsNullOrWhiteSpace($trimmed)) {
    $normalizedInputs += $trimmed
  }
}

if ($normalizedInputs.Count -eq 0) {
  throw "BuildInputPaths must not be empty."
}

$statusArgs = @("-C", $RootDir, "status", "--short", "--") + $normalizedInputs
$status = (& git @statusArgs) -join "`n"
if (![string]::IsNullOrWhiteSpace($status)) {
  throw "Release candidate build input paths are not clean:`n$status"
}

$evidenceDir = Split-Path -Parent $EvidencePath
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

$lines = @(
  "# Release candidate evidence",
  "",
  "- generated_at: $(Get-Date -Format o)",
  "- full_commit_sha: $FullSha",
  "- short_commit_sha: $ShortSha",
  "- candidate_issues: $($CandidateIssues -join ', ')",
  "- clean_command: git status --short -- $($normalizedInputs -join ' ')",
  "- clean_result: clean for listed build input files",
  "",
  "## Build Input Files"
)

foreach ($path in $normalizedInputs) {
  $lines += "- $path"
}

Set-Content -LiteralPath $EvidencePath -Value $lines -Encoding utf8
Write-Output "release candidate evidence written: $EvidencePath"
Write-Output "full_commit_sha: $FullSha"
Write-Output "short_commit_sha: $ShortSha"
