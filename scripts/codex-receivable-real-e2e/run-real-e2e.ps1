param(
  [int]$BackendPort = 8093,
  [int]$FrontendPort = 5176
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$backend = Join-Path $root 'banquet_project'
$frontend = Join-Path $root 'frontend_v3'
$evidence = Join-Path $PSScriptRoot 'evidence'
New-Item -ItemType Directory -Path $evidence -Force | Out-Null

$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$dump = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe'
$conn = @('--protocol=TCP', '--host=127.0.0.1', '--port=13317', '--user=root')
$sourceSchema = 'e2e_ui08_1788853545'
$schema = 'cx_recv13_' + (Get-Date -Format 'yyyyMMdd_HHmmss')
$username = 'cx13_' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$password = [guid]::NewGuid().ToString('N')
$jwtSecret = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
$aesSecret = [guid]::NewGuid().ToString('N')
$backendProcess = $null
$frontendProcess = $null

function Invoke-MySql([string]$sql, [string]$database = '') {
  $args = @($conn)
  if ($database) { $args += $database }
  $args += @('--batch', '--skip-column-names', '--default-character-set=utf8mb4')
  if ($sql.Contains("`n")) { $output = $sql | & $mysql @args }
  else { $output = & $mysql @args '-e' $sql }
  if ($LASTEXITCODE -ne 0) { throw "mysql failed with exit $LASTEXITCODE" }
  return $output
}

function Stop-ProcessTree([int]$processId) {
  $children = Get-CimInstance Win32_Process -Filter "ParentProcessId=$processId" -ErrorAction SilentlyContinue
  foreach ($child in $children) { Stop-ProcessTree ([int]$child.ProcessId) }
  Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

function Wait-Http([string]$url, [int]$seconds) {
  $deadline = (Get-Date).AddSeconds($seconds)
  do {
    try {
      $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 3
      if ($response.StatusCode -eq 200) { return }
    } catch { Start-Sleep -Milliseconds 750 }
  } while ((Get-Date) -lt $deadline)
  throw "Timed out waiting for $url"
}

try {
  foreach ($port in @($BackendPort, $FrontendPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
      throw "Port $port is already in use"
    }
  }

  Invoke-MySql "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
  $dumpArgs = @($conn) + @('--no-data', '--skip-add-drop-table', '--skip-comments', '--single-transaction',
    '--default-character-set=utf8mb4', $sourceSchema)
  & $dump @dumpArgs | & $mysql @conn '--default-character-set=utf8mb4' $schema
  if ($LASTEXITCODE -ne 0) { throw 'schema clone failed' }
  $migration = (Resolve-Path (Join-Path $root 'scripts\migrations\receivable_payment_request_v1.sql')).Path.Replace('\', '/')
  & $mysql @conn $schema '--default-character-set=utf8mb4' '-e' "source $migration"
  if ($LASTEXITCODE -ne 0) { throw 'receivable migration failed' }

  $seed = @"
INSERT INTO store_info(store_id,store_code,store_name,store_short_name,store_type,status,sort_order,created_at)
VALUES(1,'CX13','CX13_TEST_STORE','CX13','test','open',1,NOW());
INSERT INTO staff_master(staff_id,staff_name,staff_account,staff_password,employment_status,role,store_id,department,staff_position,permission_level,created_at)
VALUES(913001,'CX13_TEST_MANAGER','$username','$password','active','store_manager',1,'FINANCE','TEST_MANAGER',9,NOW());
INSERT INTO customer_master(customer_id,customer_name,store_id,is_active,created_at)
VALUES(11,'CX13_TEST_CUSTOMER',1,1,NOW());
INSERT INTO booking_master(id,booking_id,booking_no,customer_id,customer_name,store_id,status,created_at)
VALUES(913001,'BK-CX13-1','BN-CX13-1',11,'CX13_TEST_CUSTOMER',1,'confirmed',NOW());
INSERT INTO finance_account(account_id,account_code,account_name,account_type,is_active,store_id,created_at)
VALUES(101,'CX13-ACTIVE','CX13_ACTIVE_ACCOUNT','cash',b'1',1,NOW()),
      (103,'CX13-DISABLED','CX13_DISABLED_ACCOUNT','cash',b'0',1,NOW());
"@
  Invoke-MySql $seed $schema | Out-Null

  Push-Location $backend
  try { & mvn.cmd -o -q -DskipTests package } finally { Pop-Location }
  if ($LASTEXITCODE -ne 0) { throw 'backend package failed' }

  $sourceHead = (& git -C $root rev-parse HEAD).Trim()
  $classHash = (Get-FileHash -Algorithm SHA256 (Join-Path $backend 'target\classes\com\youjian\banquet\service\ReceivablePaymentService.class')).Hash
  [ordered]@{
    sourceHead = $sourceHead
    baseSha = '09a15bbbb7b0d0eba3335fb45f830499cacf5405'
    backendRuleSource = '0fb2fce02649e0ca91e88b4c66a25db6d2e0b509'
    frontendCandidateSource = 'c3750bb006c555e995568b102dc6b58ed9455e10'
    classSha256 = $classHash
    migrationSha256 = (Get-FileHash -Algorithm SHA256 (Join-Path $root 'scripts\migrations\receivable_payment_request_v1.sql')).Hash
    schema = $schema
    mysql = '127.0.0.1:13317'
    backend = "http://127.0.0.1:$BackendPort"
    frontend = "http://127.0.0.1:$FrontendPort"
    production = $false
  } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence 'environment.json') -Encoding utf8

  $env:JWT_SECRET = $jwtSecret
  $env:AES_SECRET_KEY = $aesSecret
  Set-Content -LiteralPath (Join-Path $evidence 'backend.stdout.log') -Value '' -Encoding utf8
  Set-Content -LiteralPath (Join-Path $evidence 'backend.stderr.log') -Value '' -Encoding utf8
  $backendArgs = @('-jar', (Join-Path $backend 'target\banquet-1.0.0.jar'), '--spring.profiles.active=prod',
    "--server.port=$BackendPort", "--spring.datasource.url=jdbc:mysql://127.0.0.1:13317/$schema`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&allowMultiQueries=true&characterEncoding=utf-8",
    '--spring.datasource.username=root', '--spring.datasource.password=', '--spring.jpa.hibernate.ddl-auto=none',
    "--cors.allowed-origins=http://127.0.0.1:$FrontendPort", '--app.notify.enabled=false', '--legal.enabled=false')
  $backendProcess = Start-Process -FilePath 'java.exe' -ArgumentList $backendArgs -WorkingDirectory $backend -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $evidence 'backend.stdout.log') -RedirectStandardError (Join-Path $evidence 'backend.stderr.log')
  Wait-Http "http://127.0.0.1:$BackendPort/api/actuator/health" 120

  Set-Content -LiteralPath (Join-Path $evidence 'frontend.stdout.log') -Value '' -Encoding utf8
  Set-Content -LiteralPath (Join-Path $evidence 'frontend.stderr.log') -Value '' -Encoding utf8
  $frontendStdin = Join-Path $evidence 'frontend.stdin'
  Set-Content -LiteralPath $frontendStdin -Value '' -Encoding ascii
  $viteEntry = Join-Path $frontend 'node_modules\vite\bin\vite.js'
  $frontendProcess = Start-Process -FilePath 'node.exe' -ArgumentList @($viteEntry,'--host','127.0.0.1','--port',"$FrontendPort") `
    -WorkingDirectory $frontend -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $evidence 'frontend.stdout.log') `
    -RedirectStandardError (Join-Path $evidence 'frontend.stderr.log') -RedirectStandardInput $frontendStdin
  Wait-Http "http://127.0.0.1:$FrontendPort/login" 90

  $env:CX13_FRONTEND = "http://127.0.0.1:$FrontendPort"
  $env:CX13_BACKEND = "http://127.0.0.1:$BackendPort"
  $env:CX13_USERNAME = $username
  $env:CX13_PASSWORD = $password
  $env:CX13_SCHEMA = $schema
  $env:CX13_EVIDENCE = $evidence
  $env:PLAYWRIGHT_MODULE = 'F:\solo\artifacts\team-worktrees\trae\scripts\trae-cost-e2e\node_modules\playwright'
  & node (Join-Path $PSScriptRoot 'real-browser-e2e.mjs')
  if ($LASTEXITCODE -ne 0) { throw "browser e2e failed with exit $LASTEXITCODE" }

  $ids = Get-Content -Raw (Join-Path $evidence 'business-ids.json') | ConvertFrom-Json
  $dbRows = Invoke-MySql ("SELECT r.receivable_id,r.receivable_no,r.total_amount,r.received_amount,r.pending_amount,r.status," +
    "COUNT(p.payment_id),COALESCE(SUM(p.amount),0) FROM finance_receivable r LEFT JOIN finance_payment_record p " +
    "ON p.receivable_id=r.receivable_id AND p.store_id=r.store_id WHERE r.receivable_id=" + [long]$ids.receivableId +
    " GROUP BY r.receivable_id,r.receivable_no,r.total_amount,r.received_amount,r.pending_amount,r.status") $schema
  $parts = ($dbRows | Select-Object -First 1) -split "`t"
  $requests = Invoke-MySql "SELECT request_id,op_type,target_id,target_no FROM receivable_payment_request ORDER BY created_at" $schema
  [ordered]@{
    receivableId = [long]$parts[0]
    receivableNo = $parts[1]
    total = $parts[2]
    received = $parts[3]
    pending = $parts[4]
    status = $parts[5]
    paymentCount = [int]$parts[6]
    paymentSum = $parts[7]
    conserved = ([decimal]$parts[2] -eq ([decimal]$parts[3] + [decimal]$parts[4]) -and [decimal]$parts[3] -eq [decimal]$parts[7])
    idempotencyRows = @($requests).Count
    schema = $schema
  } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidence 'db-assertions.json') -Encoding utf8

  Write-Output "CX13_REAL_E2E_OK schema=$schema"
} finally {
  if ($frontendProcess) { Stop-ProcessTree $frontendProcess.Id }
  if ($backendProcess) { Stop-ProcessTree $backendProcess.Id }
  Remove-Item Env:CX13_PASSWORD -ErrorAction SilentlyContinue
  Remove-Item Env:CX13_USERNAME -ErrorAction SilentlyContinue
  Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
  Remove-Item Env:AES_SECRET_KEY -ErrorAction SilentlyContinue
}
