param(
    [ValidateSet("all", "nacos", "stable-versions", "release-note-db", "release-ref", "admin-build", "client-test")]
    [string] $Check = "all",
    [ValidateSet("test", "prod")]
    [string] $Environment = "test",
    [string] $TestHost = "manman",
    [string] $ProdHost = "manman2",
    [string] $BaseUrl = "http://111.228.39.103",
    [int] $TimeoutSeconds = 20
)

$ErrorActionPreference = "Continue"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$timestamp = "{0}-{1}" -f (Get-Date -Format "yyyyMMdd-HHmmss-fff"), ([guid]::NewGuid().ToString("N").Substring(0, 8))
$logDir = Join-Path $repoRoot "tmp/release-gates/$timestamp"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Invoke-Logged {
    param(
        [Parameter(Mandatory = $true)][string] $Name,
        [Parameter(Mandatory = $true)][scriptblock] $Command
    )

    $path = Join-Path $logDir "$Name.log"
    "START $(Get-Date -Format o)" | Tee-Object -FilePath $path | Out-Null
    try {
        & $Command *>> $path
        $exit = $LASTEXITCODE
        if ($null -eq $exit) {
            $exit = 0
        }
    } catch {
        $_ | Out-String | Add-Content -Path $path
        $exit = 1
    }
    "END $(Get-Date -Format o) exit=$exit" | Tee-Object -FilePath $path -Append | Out-Null
    return [pscustomobject]@{ name = $Name; exit = $exit; log = $path }
}

function Test-Selected {
    param([string] $Name)
    return $Check -eq "all" -or $Check -eq $Name
}

$targetHost = if ($Environment -eq "prod") { $ProdHost } else { $TestHost }
$nacosContainer = if ($Environment -eq "prod") { "yudao-nacos-prod" } else { "yudao-nacos" }
$mysqlContainer = if ($Environment -eq "prod") { "yudao-mysql-prod" } else { "yudao-mysql" }
$dbName = "ruoyi-vue-pro"
$results = @()

if (Test-Selected "nacos") {
    $results += Invoke-Logged "nacos-docker-health" {
        ssh -o BatchMode=yes -o ConnectTimeout=$TimeoutSeconds $targetHost "docker inspect $nacosContainer --format '{{json .State.Health}}'; docker ps --filter name=$nacosContainer --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'; docker logs --tail=120 $nacosContainer 2>&1"
    }
    $results += Invoke-Logged "nacos-console-readiness" {
        curl.exe -sS --max-time $TimeoutSeconds "$BaseUrl`:8080/v3/console/health/readiness"
    }
    $results += Invoke-Logged "nacos-service-instance" {
        curl.exe -sS --max-time $TimeoutSeconds "$BaseUrl`:8848/nacos/v1/ns/instance/list?namespaceId=dev&groupName=DEFAULT_GROUP&serviceName=system-server"
    }
}

if (Test-Selected "stable-versions") {
    $results += Invoke-Logged "git-release-versions" {
        git -C $repoRoot rev-parse HEAD
        git -C $repoRoot rev-parse --short=12 HEAD
        git -C $repoRoot tag --sort=-creatordate | Select-String '^prod-stable-' | Select-Object -First 10
    }
    $results += Invoke-Logged "frontend-running-images-test" {
        $remoteCommand = 'client=$(docker inspect draw2video-client --format ''{{.Config.Image}}''); admin=$(docker inspect draw2video-admin --format ''{{.Config.Image}}''); echo client=$client; echo admin=$admin; docker images --format ''{{.Repository}}:{{.Tag}}'' | grep -E ''draw2video-(client|admin)'' | head -20; case "$client" in *:test-[0-9a-f]*) ;; *) echo invalid-test-client-tag=$client; exit 2;; esac; case "$admin" in *:test-[0-9a-f]*) ;; *) echo invalid-test-admin-tag=$admin; exit 2;; esac'
        ssh -o BatchMode=yes -o ConnectTimeout=$TimeoutSeconds $TestHost $remoteCommand
    }
    $results += Invoke-Logged "frontend-running-images-prod" {
        $remoteCommand = 'client=$(docker inspect draw2video-client --format ''{{.Config.Image}}''); admin=$(docker inspect draw2video-admin --format ''{{.Config.Image}}''); echo client=$client; echo admin=$admin; docker images --format ''{{.Repository}}:{{.Tag}}'' | grep -E ''draw2video-(client|admin)'' | head -20; case "$client" in *:prod-[0-9a-f]*) ;; *) echo invalid-prod-client-tag=$client; exit 2;; esac; case "$admin" in *:prod-[0-9a-f]*) ;; *) echo invalid-prod-admin-tag=$admin; exit 2;; esac'
        ssh -o BatchMode=yes -o ConnectTimeout=$TimeoutSeconds $ProdHost $remoteCommand
    }
}

if (Test-Selected "release-note-db") {
    $results += Invoke-Logged "release-note-sql-files" {
        git -C $repoRoot rev-parse --short=12 HEAD
        git -C $repoRoot ls-files sql/mysql/model/model_db.sql sql/mysql/system/aigc_admin_menu.sql
        Select-String -Path (Join-Path $repoRoot "sql/mysql/model/model_db.sql") -Pattern "CREATE TABLE ``aigc_release_note``|idx_status_release_date|uk_version"
        Select-String -Path (Join-Path $repoRoot "sql/mysql/system/aigc_admin_menu.sql") -Pattern "aigc:release-note:(query|create|update|publish|delete)"
    }
    $results += Invoke-Logged "release-note-db-verify" {
        ssh -o BatchMode=yes -o ConnectTimeout=$TimeoutSeconds $targetHost "docker exec $mysqlContainer mysql -uroot -p123456 -D $dbName -e \"SHOW CREATE TABLE aigc_release_note; SELECT permission,type,deleted FROM system_menu WHERE permission LIKE 'aigc:release-note:%' ORDER BY permission,type;\""
    }
}

if (Test-Selected "release-ref") {
    $results += Invoke-Logged "release-ref-diff" {
        git -C $repoRoot rev-parse HEAD
        git -C $repoRoot log --oneline -12
        git -C $repoRoot status --short --branch
    }
}

if (Test-Selected "admin-build") {
    $results += Invoke-Logged "draw2video-admin-build-test" {
        Push-Location (Join-Path $repoRoot "yudao-ui/draw2video-admin")
        try {
            pnpm build:test
        } finally {
            Pop-Location
        }
    }
}

if (Test-Selected "client-test") {
    $results += Invoke-Logged "draw2video-client-pnpm-test" {
        Push-Location (Join-Path $repoRoot "yudao-ui/draw2video-client")
        try {
            pnpm test
        } finally {
            Pop-Location
        }
    }
}

$summary = Join-Path $logDir "summary.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -Path $summary -Encoding utf8
"summary=$summary"
$failed = @($results | Where-Object { $_.exit -ne 0 })
if ($failed.Count -gt 0) {
    exit 1
}
exit 0
