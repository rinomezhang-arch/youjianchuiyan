param(
    [Parameter(Mandatory = $true)][string]$PlaywrightModule,
    [Parameter(Mandatory = $true)][string]$ManifestPath,
    [int]$BackendPort = 18083,
    [int]$WebPort = 5184
)
# CO-RECEIPT-R1-29: replay preserved fixtures and prebuilt, source-bound artifacts.
# No default seed, build, test, install, database restart or shared-process stop.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$backend = Join-Path $root 'banquet_project'
$manifest = (Resolve-Path -LiteralPath $ManifestPath).Path
$schema = 'co_print23_20260909_022305'
$mysql = 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe'
$freeVirtualGb = [math]::Round((Get-CimInstance Win32_OperatingSystem).FreeVirtualMemory / 1MB, 2)
if ($freeVirtualGb -lt 2.0) { throw "Free virtual memory below 2GB: $freeVirtualGb" }
foreach ($port in @($BackendPort,$WebPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "Port $port in use; refusing to stop its owner" }
}
if (-not (Get-NetTCPConnection -State Listen -LocalPort 13317 -ErrorAction SilentlyContinue)) { throw 'Preserved local MySQL is not listening' }
& node (Join-Path $PSScriptRoot 'receipt-artifacts.mjs') verify $manifest
if ($LASTEXITCODE -ne 0) { throw 'Source/dist/jar binding failed; no services started' }
$fixtureSql = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'seed-tr24-accounts.sql') -Raw -Encoding utf8
$count = & $mysql --no-defaults --protocol=tcp --host=127.0.0.1 --port=13317 --user=root --batch --skip-column-names --default-character-set=utf8mb4 $schema -e $fixtureSql
if ($LASTEXITCODE -ne 0 -or "$count".Trim() -ne '4') { throw 'Existing account fixtures are missing; no seed performed' }
$runId = (Get-Date -Format 'yyyyMMdd-HHmmssfff') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
# Chinese directory uses Unicode codepoints to keep Windows PowerShell parsing deterministic.
$collab = [string][char]0x534F + [char]0x4F5C
$runParent = Join-Path $root "docs/$collab/Codex/receipt-r1-29/evidence"
$evidence = Join-Path $runParent $runId
New-Item -ItemType Directory -Path $evidence | Out-Null
$names = @('JWT_SECRET','AES_SECRET_KEY','TR24_WEB_PORT','TR24_API_PORT','PLAYWRIGHT_MODULE','TR24_WEB_BASE','TR24_SCHEMA','TR24_EVIDENCE','TR29_MANIFEST')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$webProcess = $null
$backendProcess = $null
$driverExit = $null
$started = (Get-Date).ToString('o')
try {
    $env:JWT_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $env:AES_SECRET_KEY = [guid]::NewGuid().ToString('N')
    $jarArgs = @('-Xmx512m','-jar',(Join-Path $backend 'target/banquet-1.0.0.jar'),'--spring.profiles.active=prod',
        "--server.port=$BackendPort",
        "--spring.datasource.url=jdbc:mysql://127.0.0.1:13317/$schema`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8",
        '--spring.datasource.username=root','--spring.datasource.password=',
        '--spring.jpa.hibernate.ddl-auto=none','--app.notify.enabled=false','--legal.enabled=false')
    $backendProcess = Start-Process -FilePath java.exe -ArgumentList $jarArgs -WorkingDirectory $backend -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $evidence 'backend.stdout.log') -RedirectStandardError (Join-Path $evidence 'backend.stderr.log')
    $deadline = (Get-Date).AddSeconds(120)
    $healthy = $false
    do {
        if ($backendProcess.HasExited) { throw 'Task-owned backend exited before readiness' }
        try { $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$BackendPort/api/actuator/health" -TimeoutSec 3
              if ($response.StatusCode -eq 200) { $healthy = $true; break } } catch {}
        Start-Sleep -Milliseconds 750
    } while ((Get-Date) -lt $deadline)
    if (-not $healthy) { throw 'Backend readiness timed out' }
    $env:TR24_WEB_PORT = "$WebPort"
    $env:TR24_API_PORT = "$BackendPort"
    $webProcess = Start-Process -FilePath node.exe -ArgumentList @((Join-Path $PSScriptRoot 'serve-tr24-web.mjs')) -WorkingDirectory $root -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $evidence 'web.stdout.log') -RedirectStandardError (Join-Path $evidence 'web.stderr.log')
    $deadline = (Get-Date).AddSeconds(20)
    $webReady = $false
    do {
        try { $response = Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$WebPort/login" -TimeoutSec 3
              if ($response.StatusCode -eq 200) { $webReady = $true; break } } catch {}
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    if (-not $webReady) { throw 'Web readiness timed out' }
    $env:PLAYWRIGHT_MODULE = $PlaywrightModule
    $env:TR24_WEB_BASE = "http://127.0.0.1:$WebPort"
    $env:TR24_SCHEMA = $schema
    $env:TR24_EVIDENCE = $evidence
    $env:TR29_MANIFEST = $manifest
    & node (Join-Path $PSScriptRoot 'tr24-receipt-browser.mjs') 1> (Join-Path $evidence 'driver.stdout.log') 2> (Join-Path $evidence 'driver.stderr.log')
    $driverExit = $LASTEXITCODE
    if ($driverExit -ne 0) { throw "Driver failed ($driverExit); retained evidence: $evidence" }
} finally {
    if ($webProcess -and -not $webProcess.HasExited) { Stop-Process -Id $webProcess.Id -ErrorAction SilentlyContinue }
    if ($backendProcess -and -not $backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue }
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    @{task='CO-RECEIPT-R1-29';started=$started;finished=(Get-Date).ToString('o');driverExit=$driverExit;backendPid=$backendProcess.Id;webPid=$webProcess.Id;manifest=$manifest} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence 'run.json') -Encoding utf8
    Write-Output "TR29_EVIDENCE=$evidence"
}
Write-Output 'TR29_RUNNER_OK'
