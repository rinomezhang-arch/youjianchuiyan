param(
    [Parameter(Mandatory = $true)]
    [string]$PlaywrightModule,
    [string]$Schema = 'co_print23_20260909_022305',
    [string]$Order = 'COPRINT23-BK-001',
    [int]$BackendPort = 18083,
    [int]$WebPort = 5184
)

# TR-RECEIPT-REAL-24 replayable runner.
# Reuses the shared 13317 MySQL and the PRESERVED schema (no setup/reinstall/restart):
#   seed TR24 synthetic accounts (idempotent) -> targeted BillReceiptTest ->
#   candidate jar build -> backend 18083 (-Xmx512m) -> dist proxy 5184 ->
#   Playwright real-click driver.
# Note: this file is intentionally ASCII-only (runtime finds the Chinese-named
# collaboration dir), so both Windows PowerShell 5.1 and pwsh 7 behave the same.
$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$backend = Join-Path $root 'banquet_project'
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$seedFile = Join-Path $PSScriptRoot 'seed-tr24-accounts.sql'
# Find the Chinese-named collaboration dir at runtime (do not hardcode it).
$collabDir = Get-ChildItem (Join-Path $root 'docs') -Directory |
    Where-Object { $_.Name -notmatch '^[A-Za-z0-9_\-]+$' } |
    Select-Object -First 1
if (-not $collabDir) { throw 'collaboration docs directory not found' }
$evidence = Join-Path $collabDir.FullName 'Trae\receipt-real-24\evidence'
New-Item -ItemType Directory -Path $evidence -Force | Out-Null

function Invoke-Native {
    param([string]$FilePath, [string[]]$ArgList, [string]$WorkingDirectory, [string]$LogName, [string]$StdInFile)
    $out = Join-Path $evidence ($LogName + '.stdout.log')
    $err = Join-Path $evidence ($LogName + '.stderr.log')
    $psi = @{
        FilePath = $FilePath
        ArgumentList = $ArgList
        Wait = $true
        PassThru = $true
        NoNewWindow = $true
        RedirectStandardOutput = $out
        RedirectStandardError = $err
    }
    if ($WorkingDirectory) { $psi.WorkingDirectory = $WorkingDirectory }
    if ($StdInFile) { $psi.RedirectStandardInput = $StdInFile }
    $p = Start-Process @psi
    if ($p.ExitCode -ne 0) { throw "$LogName failed with exit code $($p.ExitCode); see $out / $err" }
}

$freeVirtualGb = [math]::Round((Get-CimInstance Win32_OperatingSystem).FreeVirtualMemory / 1MB, 2)
if ($freeVirtualGb -lt 2.0) { throw "TR24 precondition failed: free virtual memory ${freeVirtualGb}GB below 2.0GB" }
foreach ($p in @($BackendPort, $WebPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue) { throw "Port $p is already in use" }
}
if (-not (Get-NetTCPConnection -State Listen -LocalPort 13317 -ErrorAction SilentlyContinue)) { throw 'Shared MySQL 13317 is not listening; this runner will NOT restart it' }

# 1) TR24 synthetic accounts (idempotent, scoped to the reused schema)
Invoke-Native -FilePath $mysql -StdInFile $seedFile -LogName 'seed' -ArgList @(
    '--no-defaults','--protocol=tcp','--host=127.0.0.1','--port=13317','--user=root',
    '--default-character-set=utf8mb4', $Schema)

# 2) Targeted backend tests only (no payroll/iPad suites)
$env:MAVEN_OPTS = '-Xmx256m'
Invoke-Native -FilePath 'cmd.exe' -WorkingDirectory $backend -LogName 'mvn-test' -ArgList @(
    '/c','mvn.cmd','-o','-q','-Dtest=BillReceiptTest','test')

# 3) Candidate jar
Invoke-Native -FilePath 'cmd.exe' -WorkingDirectory $backend -LogName 'mvn-package' -ArgList @(
    '/c','mvn.cmd','-o','-q','-DskipTests','package')

# 4) Backend on 18083 against the preserved schema (read-mostly; ddl-auto none)
$env:JWT_SECRET = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
$env:AES_SECRET_KEY = [guid]::NewGuid().ToString('N')
$jar = Join-Path $backend 'target\banquet-1.0.0.jar'
$jarArgs = @('-Xmx512m','-jar',$jar,'--spring.profiles.active=prod',
    "--server.port=$BackendPort",
    "--spring.datasource.url=jdbc:mysql://127.0.0.1:13317/$Schema`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8",
    '--spring.datasource.username=root','--spring.datasource.password=',
    '--spring.jpa.hibernate.ddl-auto=none','--app.notify.enabled=false','--legal.enabled=false')

$webProcess = $null
$backendProcess = $null
try {
    $backendProcess = Start-Process -FilePath 'java.exe' -ArgumentList $jarArgs -WorkingDirectory $backend -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $evidence 'backend.stdout.log') -RedirectStandardError (Join-Path $evidence 'backend.stderr.log')

    $deadline = (Get-Date).AddSeconds(120)
    $healthy = $false
    do {
        try { $r = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$BackendPort/api/actuator/health" -TimeoutSec 3
              if ($r.StatusCode -eq 200) { $healthy = $true; break } } catch {}
        Start-Sleep -Milliseconds 750
    } while ((Get-Date) -lt $deadline)
    if (-not $healthy) { throw 'backend did not become healthy within 120 seconds' }

    # 5) dist static + /api proxy (same origin; Origin rewritten to prod domain)
    $env:TR24_WEB_PORT = "$WebPort"
    $env:TR24_API_PORT = "$BackendPort"
    $webProcess = Start-Process -FilePath 'node.exe' -ArgumentList @((Join-Path $PSScriptRoot 'serve-tr24-web.mjs')) -WorkingDirectory $root -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $evidence 'web.stdout.log') -RedirectStandardError (Join-Path $evidence 'web.stderr.log')
    $webDeadline = (Get-Date).AddSeconds(20)
    $webOk = $false
    do {
        try { $r = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$WebPort/login" -TimeoutSec 3
              if ($r.StatusCode -eq 200) { $webOk = $true; break } } catch {}
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $webDeadline)
    if (-not $webOk) { throw 'web proxy did not become ready within 20 seconds' }

    # 6) Playwright real-click driver (single browser context)
    $env:PLAYWRIGHT_MODULE = $PlaywrightModule
    $env:TR24_WEB_BASE = "http://127.0.0.1:$WebPort"
    $env:TR24_SCHEMA = $Schema
    $env:TR24_ORDER = $Order
    $env:TR24_EVIDENCE = $evidence
    Invoke-Native -FilePath 'node.exe' -WorkingDirectory $root -LogName 'driver' -ArgList @(
        (Join-Path $PSScriptRoot 'tr24-receipt-browser.mjs'))
} finally {
    if ($webProcess -and -not $webProcess.HasExited) { Stop-Process -Id $webProcess.Id -Force -ErrorAction SilentlyContinue }
    if ($backendProcess -and -not $backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue }
    $env:JWT_SECRET = $null
    $env:AES_SECRET_KEY = $null
    $env:MAVEN_OPTS = $null
    $env:PLAYWRIGHT_MODULE = $null
    $env:TR24_WEB_BASE = $null
    $env:TR24_SCHEMA = $null
    $env:TR24_ORDER = $null
    $env:TR24_EVIDENCE = $null
}

Write-Output "TR24_RUNNER_OK schema=$Schema order=$Order backend=$BackendPort web=$WebPort evidence=$evidence"
