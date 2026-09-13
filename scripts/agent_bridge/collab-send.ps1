param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Claude', 'Trae', 'Tianlong', 'Dilong', 'Weilong', 'Codex')]
    [string]$Target,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Message,

    [ValidateRange(4, 60)]
    [int]$VerifyTimeoutSeconds = 20,

    [ValidatePattern('^[A-Za-z0-9_-]{8,128}$')]
    [string]$SessionId,

    [string]$ConversationKey,

    [string]$GatewayUrl,

    [string]$BridgeTarget,

    [switch]$WaitForDelivery
)

$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot)
$dispatchRoot = Join-Path $root 'artifacts\coordination-r3\dispatch'
$pending = Join-Path $dispatchRoot 'pending'
$sent = Join-Path $dispatchRoot 'sent'
New-Item -ItemType Directory -Path $pending, $sent -Force | Out-Null

$dispatchId = 'dispatch-' + [guid]::NewGuid().ToString()
$marker = 'CX-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$started = Get-Date

function Invoke-BridgeDispatch {
    param([string]$BridgeTarget)
    $requestPath = Join-Path $pending ($dispatchId + '.json')
    $request = @{
        name = 'bridge_send_message'
        arguments = @{
            machine = 'local'
            target = $BridgeTarget
            from_target = 'codex/current'
            message = "[$marker] $Message"
            relay_summary = "Codex统筹已向$Target发送一条项目协作消息。"
            ttl = 86400
        }
    }
    $request | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $requestPath -Encoding utf8
    $raw = & node (Join-Path $PSScriptRoot 'bridge-client.mjs') $requestPath
    if ($LASTEXITCODE -ne 0) { throw "Agent Bridge dispatch failed for $Target" }
    Move-Item -LiteralPath $requestPath -Destination (Join-Path $sent ($dispatchId + '.json'))
    $id = [regex]::Match(($raw -join "`n"), 'msg-[0-9a-f-]+').Value
    [pscustomobject]@{
        target = $Target
        transport = 'agent-bridge'
        dispatchId = $dispatchId
        messageId = $id
        state = 'queued'
        note = '落盘回执已取得；执行端消费以任务板或对方回信为准'
        elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
    }
}

function Invoke-GatewayDispatch {
    param([string]$Url, [string]$SessionKey)
    $config = Get-Content -LiteralPath "$HOME\.openclaw\openclaw.json" -Raw | ConvertFrom-Json
    $token = [string]$config.gateway.auth.token
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'OpenClaw gateway credential is unavailable' }
    $body = "[$marker] $Message"
    $params = @{
        sessionKey = $SessionKey
        message = $body
        idempotencyKey = [guid]::NewGuid().ToString()
    } | ConvertTo-Json -Compress
    $ack = & openclaw gateway call agent --url $Url --token $token --params $params --json 2>$null
    if ($LASTEXITCODE -ne 0 -or ($ack -join "`n") -notmatch 'accepted|in_flight|runId') {
        throw "OpenClaw gateway rejected dispatch for $Target"
    }

    $verified = $false
    if ($WaitForDelivery) {
        $deadline = (Get-Date).AddSeconds($VerifyTimeoutSeconds)
        do {
            Start-Sleep -Milliseconds 800
            $getParams = @{ sessionKey = $SessionKey } | ConvertTo-Json -Compress
            $history = & openclaw gateway call sessions.get --url $Url --token $token --params $getParams --json 2>$null
            if ($LASTEXITCODE -eq 0 -and ($history -join "`n").Contains($marker)) { $verified = $true; break }
        } while ((Get-Date) -lt $deadline)
    }

    [pscustomobject]@{
        target = $Target
        transport = 'openclaw-gateway'
        dispatchId = $dispatchId
        marker = $marker
        state = if ($verified) { 'delivered' } else { 'accepted' }
        note = if ($verified) { '已在目标真实会话回读到消息标识' } else { '网关已接收；由后台监管继续核对会话或任务板回执' }
        elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
    }
}

function Invoke-TraeDirectDispatch {
    if ([string]::IsNullOrWhiteSpace($SessionId)) {
        throw 'TRAE_SESSION_ID_REQUIRED'
    }
    $descriptorPath = Join-Path $root 'artifacts\bridge-evidence-20260907\trae-workspace\adapter-private.json'
    if (-not (Test-Path -LiteralPath $descriptorPath)) {
        throw 'TRAE_ADAPTER_DESCRIPTOR_MISSING'
    }
    $descriptor = Get-Content -LiteralPath $descriptorPath -Raw | ConvertFrom-Json
    $headers = @{ Authorization = 'Bearer ' + [string]$descriptor.token }
    $status = Invoke-RestMethod -Uri ([string]$descriptor.url + '/status') -Headers $headers -Method Get -TimeoutSec 8
    if ([string]$status.sessionId -ne $SessionId) {
        throw 'TRAE_SESSION_MISMATCH'
    }
    if (-not $status.ready -or $status.busy) {
        throw 'TRAE_SESSION_BUSY_OR_NOT_READY'
    }
    $nativeId = 'cx-' + [guid]::NewGuid().ToString('N')
    $body = @{
        id = $nativeId
        prompt = "[$marker] $Message"
        expectedSessionId = $SessionId
    } | ConvertTo-Json -Compress
    try {
        $reply = Invoke-RestMethod -Uri ([string]$descriptor.url + '/message') -Headers ($headers + @{ 'Content-Type' = 'application/json' }) -Method Post -Body $body -TimeoutSec 1800
        [pscustomobject]@{
            target = $Target
            transport = 'trae-native-session'
            dispatchId = $dispatchId
            marker = $marker
            state = if ($reply.passed) { 'delivered' } else { 'uncertain' }
            note = if ($reply.passed) { '指定会话已返回响应' } else { '原生适配器未确认响应；禁止盲目重发' }
            elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
        }
    } catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        [pscustomobject]@{
            target = $Target
            transport = 'trae-native-session'
            dispatchId = $dispatchId
            marker = $marker
            state = 'uncertain'
            note = "原生适配器返回 HTTP $statusCode；检查指定会话记录后再决定是否重发"
            elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
        }
    }
}

switch ($Target) {
    'Claude' { Invoke-BridgeDispatch $(if ($BridgeTarget) { $BridgeTarget } else { 'claude-code/solo' }) }
    'Trae' { Invoke-TraeDirectDispatch }
    'Codex' { Invoke-BridgeDispatch $(if ($BridgeTarget) { $BridgeTarget } else { 'codex/current' }) }
    'Dilong' { Invoke-GatewayDispatch $(if ($GatewayUrl) { $GatewayUrl } else { 'ws://127.0.0.1:18789' }) $(if ($ConversationKey) { $ConversationKey } else { 'agent:main:main' }) }
    'Tianlong' { Invoke-GatewayDispatch $(if ($GatewayUrl) { $GatewayUrl } else { 'ws://100.70.215.11:11500' }) $(if ($ConversationKey) { $ConversationKey } else { 'agent:main:main' }) }
    'Weilong' {
        $sessionKey = & node (Join-Path $PSScriptRoot 'resolve-weilong-session.mjs')
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($sessionKey)) {
            throw 'Unable to resolve Weilong active session'
        }
        Invoke-GatewayDispatch 'ws://127.0.0.1:18789' ([string]$sessionKey)
    }
}
