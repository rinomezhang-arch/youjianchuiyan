param(
    [Parameter(Mandatory = $true)]
    [string]$PlaywrightModule,
    [string]$Schema = 'co_print23_20260909_022305',
    [string]$Order = 'COPRINT23-BK-001',
    [int]$BackendPort = 18083,
    [int]$WebPort = 5184,
    [switch]$InitSeed
)

# TR-RECEIPT-REAL-24 r2 replayable runner.
# Reuses the shared 13317 MySQL and the PRESERVED schema (no setup/reinstall/restart):
#   read-only preflight (TR24 accounts + standard tables/columns; -InitSeed applies the
#   INSERT-only fixtures once) -> manifest gate (bound source HEAD + dist hash) ->
#   targeted BillReceiptTest -> candidate jar build -> backend 18083 (-Xmx512m) ->
#   dist proxy 5184 -> Playwright real-click driver.
# Every run writes to a UNIQUE run dir; prior evidence is never overwritten or deleted.
# Note: this file is intentionally ASCII-only (runtime finds the Chinese-named
# collaboration dir), so both Windows PowerShell 5.1 and pwsh 7 behave the same.
$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$backend = Join-Path $root 'banquet_project'
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$seedFile = Join-Path $PSScriptRoot 'seed-tr24-accounts.sql'
$fixtureFile = Join-Path $PSScriptRoot 'init-tr24-fixtures.sql'
$manifestPath = Join-Path $PSScriptRoot 'build-manifest.json'
# Find the Chinese-named collaboration dir at runtime (do not hardcode it).
$collabDir = Get-ChildItem (Join-Path $root 'docs') -Directory |
    Where-Object { $_.Name -notmatch '^[A-Za-z0-9_\-]+$' } |
    Select-Object -First 1
if (-not $collabDir) { throw 'collaboration docs directory not found' }
$evidenceBase = Join-Path $collabDir.FullName 'Trae\receipt-real-24\evidence'
New-Item -ItemType Directory -Path $evidenceBase -Force | Out-Null
# Unique run directory: all logs/artifacts for THIS run; old evidence stays untouched.
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$rand = -join ((1..4) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
$evidence = Join-Path $evidenceBase ("runs\$stamp-$rand")
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

function Mysql-Scalar {
    param([string]$Sql)
    $mysqlArgs = @('--no-defaults','--protocol=tcp','--host=127.0.0.1','--port=13317','--user=root',
        '--default-character-set=utf8mb4','--batch','--skip-column-names', $Schema, '-e', $Sql)
    $v = & $mysql @mysqlArgs
    if ($LASTEXITCODE -ne 0) { throw "mysql query failed: $Sql" }
    return ($v | Select-Object -First 1)
}
function Test-Table { param([string]$Table)
    return [int](Mysql-Scalar "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='$Table'") -eq 1
}
function Test-Column { param([string]$Table, [string]$Column)
    return [int](Mysql-Scalar "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='$Table' AND COLUMN_NAME='$Column'") -eq 1
}

$freeVirtualGb = [math]::Round((Get-CimInstance Win32_OperatingSystem).FreeVirtualMemory / 1MB, 2)
if ($freeVirtualGb -lt 2.0) { throw "TR24 precondition failed: free virtual memory ${freeVirtualGb}GB below 2.0GB" }
foreach ($p in @($BackendPort, $WebPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue) { throw "Port $p is already in use" }
}
if (-not (Get-NetTCPConnection -State Listen -LocalPort 13317 -ErrorAction SilentlyContinue)) { throw 'Shared MySQL 13317 is not listening; this runner will NOT restart it' }

# 1) Read-only preflight: TR24 accounts + standard tables/columns must already exist.
#    Missing fixtures are only created with explicit -InitSeed (INSERT-only, no overwrite).
$standardTables = @{
    'table_master'       = @('table_id','store_id','table_number','table_name')
    'booking_table'      = @('table_booking_id','booking_id','store_id','table_id','table_name','booking_date','booking_time')
    'finance_transaction' = @('trans_id','store_id','related_type','related_no','payment_method')
}
$missingTables = @()
$mismatch = @()
foreach ($t in $standardTables.Keys) {
    if (Test-Table $t) {
        foreach ($c in $standardTables[$t]) {
            if (-not (Test-Column $t $c)) { $mismatch += "$t.$c" }
        }
    } else { $missingTables += $t }
}
$missingMasterCols = @()
foreach ($c in @('booking_time','staff_name','updated_at')) {
    if (-not (Test-Column 'booking_master' $c)) {
        if (Test-Table 'booking_master') { $missingMasterCols += $c }
    }
}
$accountCount = [int](Mysql-Scalar "SELECT COUNT(*) FROM staff_master WHERE staff_account IN ('tr24_gm','tr24_staff1','tr24_staff2')")

if ($mismatch.Count -gt 0) {
    throw "TR24 preflight STOP: existing table definition mismatch (missing columns: $($mismatch -join ', ')). Not auto-overwriting; inspect the schema manually."
}
$needFixtures = ($missingTables.Count -gt 0) -or ($missingMasterCols.Count -gt 0) -or ($accountCount -lt 3)
if ($needFixtures) {
    if (-not $InitSeed) {
        throw "TR24 preflight: fixtures missing (tables: $($missingTables -join ','); master cols: $($missingMasterCols -join ','); accounts=$accountCount/3). Re-run with -InitSeed to apply the INSERT-only seed/fixtures once; default replay never writes."
    }
    Write-Output "InitSeed: applying INSERT-only fixtures (existing rows are never overwritten)..."
    # Accounts seed is plain INSERT without DELETE: run only when accounts are missing;
    # pre-existing TR24 accounts mean a conflict, which is rejected by skipping (no overwrite).
    if ($accountCount -lt 3) {
        Invoke-Native -FilePath $mysql -StdInFile $seedFile -LogName 'seed-init' -ArgList @(
            '--no-defaults','--protocol=tcp','--host=127.0.0.1','--port=13317','--user=root',
            '--default-character-set=utf8mb4', $Schema)
    } else {
        Write-Output "InitSeed: 3 TR24 accounts already present; seed INSERT skipped (conflict not overwritten)."
    }
    # Fixtures are idempotent by design (CREATE TABLE IF NOT EXISTS / NOT EXISTS guards).
    Invoke-Native -FilePath $mysql -StdInFile $fixtureFile -LogName 'fixtures-init' -ArgList @(
        '--no-defaults','--protocol=tcp','--host=127.0.0.1','--port=13317','--user=root',
        '--default-character-set=utf8mb4', $Schema)
    $accountCount = [int](Mysql-Scalar "SELECT COUNT(*) FROM staff_master WHERE staff_account IN ('tr24_gm','tr24_staff1','tr24_staff2')")
    if ($accountCount -lt 3) { throw "InitSeed verification failed: TR24 accounts=$accountCount/3" }
    foreach ($t in $standardTables.Keys) {
        if (-not (Test-Table $t)) { throw "InitSeed verification failed: table $t still missing" }
    }
} else {
    Write-Output "Preflight read-only OK: 3 TR24 accounts, standard tables/columns present (no writes)."
}
$bindingCount = [int](Mysql-Scalar "SELECT COUNT(*) FROM booking_table WHERE booking_id='$Order' AND store_id=1 AND table_name LIKE 'TR24%'")
if ($bindingCount -lt 2) { throw "TR24 preflight: table bindings for $Order = $bindingCount (expected >=2); run with -InitSeed or inspect fixtures." }

# 2) Manifest gate: dist must be the one-off audited build, bound to a source HEAD.
if (-not (Test-Path $manifestPath)) { throw "build-manifest.json missing at $manifestPath; build frontend once and record the manifest before replay." }
$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
$distDir = Join-Path $root 'frontend_v3\dist'
if (-not (Test-Path $distDir)) { throw 'frontend_v3/dist missing; build the frontend once before replay.' }
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$hashLines = Get-ChildItem $distDir -Recurse -File | Sort-Object FullName | ForEach-Object {
    $rel = $_.FullName.Substring($distDir.Length + 1).Replace('\','/')
    $fileHash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash
    "${rel}:${fileHash}"
}
$distHash = ([BitConverter]::ToString($sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes(($hashLines -join "`n")))).Replace('-','')).ToLower()
if ($distHash -ne $manifest.distHash) {
    throw "dist hash mismatch: manifest=$($manifest.distHash) actual=$distHash. Rebuild once and rebind; replay will not serve an unbound build."
}
& git -C $root diff --quiet $manifest.sourceHead -- frontend_v3 banquet_project
if ($LASTEXITCODE -ne 0) {
    throw "frontend/backend code differs from bound sourceHead $($manifest.sourceHead); rebuild once and rebind before replay."
}
Write-Output "Manifest gate OK: sourceHead=$($manifest.sourceHead) distHash=$($distHash.Substring(0,12)) builtAt=$($manifest.builtAt)"

# 3) Targeted backend tests only (no payroll/iPad suites)
$env:MAVEN_OPTS = '-Xmx256m'
Invoke-Native -FilePath 'cmd.exe' -WorkingDirectory $backend -LogName 'mvn-test' -ArgList @(
    '/c','mvn.cmd','-o','-q','-Dtest=BillReceiptTest','test')

# 4) Candidate jar (from the same bound source tree)
Invoke-Native -FilePath 'cmd.exe' -WorkingDirectory $backend -LogName 'mvn-package' -ArgList @(
    '/c','mvn.cmd','-o','-q','-DskipTests','package')

# 5) Backend on 18083 against the preserved schema (read-mostly; ddl-auto none)
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

    # 6) dist static + /api proxy (same origin; Origin rewritten to prod domain)
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

    # 7) Playwright real-click driver (single browser context; artifacts to this run dir)
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

Write-Output "TR24_RUNNER_OK schema=$Schema order=$Order backend=$BackendPort web=$WebPort runDir=$evidence"
