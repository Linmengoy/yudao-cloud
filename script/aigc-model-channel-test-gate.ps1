param(
    [switch]$SkipPurge
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$apiArtifact = Join-Path $env:USERPROFILE ".m2\repository\cn\iocoder\cloud\yudao-module-aigc-model-api"

Set-Location $repoRoot

if (-not $SkipPurge -and (Test-Path $apiArtifact)) {
    Remove-Item -LiteralPath $apiArtifact -Recurse -Force
}

mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-api -am install -DskipTests
mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am "-Dtest=AigcModelPriceServiceImplTest,AigcModelChannelServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
