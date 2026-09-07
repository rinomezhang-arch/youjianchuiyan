# TR-OPS-COST-UI-01 可复跑验证脚本（Trae, 2026-09-07）
# 用途：对「成本配方」页面背后真实链路做接口级+数据库级断言（真实后端、真实 MySQL，非 mock）。
# 边界：只允许对本人隔离测试库执行（默认 trae_cost_e2e_20260907@127.0.0.1:13317）。
#       禁止指向生产或任何共享库。脚本会写入 TR验收_ 合成数据并触发 recalc-all 重算。
# 前置：1) 后端以 prod profile + SPRING_DATASOURCE_URL 指向隔离库运行于 8080
#       2) 已执行 trae-cost-e2e-seed.sql 注入种子数据
# 浏览器级步骤（本脚本不覆盖，见覆盖范围文档）单独以 browser-use 执行并截图留证。
param(
    [string]$ApiBase   = 'http://127.0.0.1:8080',
    [string]$DbHost    = '127.0.0.1',
    [int]   $DbPort    = 13317,
    [string]$DbName    = 'trae_cost_e2e_20260907',
    [string]$DbUser    = 'root',
    [string]$DbPass    = '',
    [string]$Username  = 'trae_test',
    [string]$Password  = '002323'
)
$ErrorActionPreference = 'Stop'
$script:pass = 0; $script:fail = 0
function Assert($name, $ok, $evidence) {
    if ($ok) { $script:pass++; Write-Output ("[PASS] {0} | {1}" -f $name, $evidence) }
    else     { $script:fail++; Write-Output ("[FAIL] {0} | {1}" -f $name, $evidence) }
}
function DbQuery($sql) {
    $env:MYSQL_PWD = $DbPass
    mysql -h $DbHost -P $DbPort -u $DbUser --default-character-set=utf8mb4 -e $sql $DbName 2>$null
}

Write-Output "=== TR-OPS-COST-UI-01 verify @ $ApiBase / $DbName@$DbHost`:$DbPort ==="

# 1) 登录（真实 staff_master 链路）
$login = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/auth/login" -ContentType 'application/json' -Body (@{username=$Username;password=$Password} | ConvertTo-Json)
Assert 'login' ($login.code -eq 200 -and $login.data.storeId -eq 1) "code=$($login.code) storeId=$($login.data.storeId) user=$($login.data.user.staffName) role=$($login.data.user.role)"
$H = @{ Authorization = "Bearer $($login.data.token)" }

# 2) /api/auth/me 会话回读
$me = Invoke-RestMethod -Uri "$ApiBase/api/auth/me" -Headers $H
Assert 'auth/me' ($me.code -eq 200 -and $me.data.storeId -eq 1) "code=$($me.code) storeId=$($me.data.storeId)"

# 3) 原料下拉数据源
$ing = Invoke-RestMethod -Uri "$ApiBase/api/ingredients?storeId=1" -Headers $H
$trIng = @($ing.data | Where-Object { $_.ingredientId -like 'TR-*' })
Assert 'ingredients TR non-empty' ($trIng.Count -ge 3) "count=$($trIng.Count) names=$(($trIng | ForEach-Object { $_.ingredientName }) -join ',')"

# 4) /api/cost/ranking 列表非空（页面主数据源，替代已废弃的 /dish-cost/*）
$rank = Invoke-RestMethod -Uri "$ApiBase/api/cost/ranking?size=500" -Headers $H
$trDishes = @($rank.data.content | Where-Object { $_.dishId -like 'TR-*' })
Assert 'cost/ranking TR dishes' ($trDishes.Count -ge 3 -and $rank.data.total -ge 3) "total=$($rank.data.total) trRows=$($trDishes.Count) first=$($trDishes[0].dishName)/sale=$($trDishes[0].salePrice)"

# 5) dishes-with-recipe 标记
$wr = Invoke-RestMethod -Uri "$ApiBase/api/recipes/dishes-with-recipe?storeId=1" -Headers $H
$wrIds = @($wr.data | ForEach-Object { $_.dishId })
Assert 'dishes-with-recipe' ($wrIds -contains 'TR-DISH-001' -and $wrIds -contains 'TR-DISH-002') "count=$($wrIds.Count) ids=$(($wrIds | Select-Object -First 5) -join ',')"

# 6) 配方明细回读（单位来自原料档案单位，下拉受限）
$rc = Invoke-RestMethod -Uri "$ApiBase/api/recipes/TR-DISH-001?storeId=1" -Headers $H
$first = $rc.data[0]
Assert 'recipe detail TR-DISH-001' ($rc.data.Count -eq 2 -and $first.unit -in @('克','毫升')) "items=$($rc.data.Count) first=$($first.ingredientName)/$($first.quantity)/$($first.unit)"

# 7) 保存成功链路：写配方 → recalc-all → 回读 + 成本价刷新
$body = @( @{ ingredientId='TR-ING-001'; quantity=500; unit='克' } ) | ConvertTo-Json -Depth 3
$s1 = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/recipes/TR-DISH-003?storeId=1" -Headers $H -ContentType 'application/json' -Body $body
Assert 'save success' ($s1.code -eq 200) "code=$($s1.code) msg=$($s1.message)"
$null = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/recipes/recalc-all" -Headers $H
$rc2 = Invoke-RestMethod -Uri "$ApiBase/api/recipes/TR-DISH-003?storeId=1" -Headers $H
$readback = $rc2.data | Where-Object { $_.ingredientId -eq 'TR-ING-001' }
Assert 'save readback' ($readback -ne $null -and [double]$readback.quantity -eq 500) "qty=$($readback.quantity) unit=$($readback.unit)"
$rank2 = Invoke-RestMethod -Uri "$ApiBase/api/cost/ranking?size=500" -Headers $H
$fish = $rank2.data.content | Where-Object { $_.dishId -eq 'TR-DISH-003' }
Assert 'recalc costPrice refreshed' ([double]$fish.costPrice -gt 0) "dish=$($fish.dishName) costPrice=$($fish.costPrice) costRate=$($fish.costRate)"

# 8) 保存失败链路：quantity 超 decimal(10,3) 上限 → code=500 显式文案 + 事务回滚（DB 不变）
$badBody = @( @{ ingredientId='TR-ING-003'; quantity=99999999; unit='毫升' } ) | ConvertTo-Json -Depth 3
$before = DbQuery "SELECT COUNT(*) c FROM dish_recipe WHERE dish_id='TR-DISH-003' AND ingredient_id='TR-ING-001';"
try {
    $resp = Invoke-WebRequest -Method Post -Uri "$ApiBase/api/recipes/TR-DISH-003?storeId=1" -Headers $H -ContentType 'application/json' -Body $badBody -UseBasicParsing
    $env2 = $resp.Content | ConvertFrom-Json
    $msgOk = ($env2.code -eq 500 -and $env2.message -like '保存配方失败*')
    Assert 'save failure explicit message' $msgOk "http=$($resp.StatusCode) code=$($env2.code) msg=$($env2.message.Substring(0,[Math]::Min(80,$env2.message.Length)))..."
} catch {
    Assert 'save failure explicit message' $false "unexpected transport error: $($_.Exception.Message)"
}
$after = DbQuery "SELECT COUNT(*) c FROM dish_recipe WHERE dish_id='TR-DISH-003' AND ingredient_id='TR-ING-001';"
$beforeJ = ($before -join "`n").Trim(); $afterJ = ($after -join "`n").Trim()
Assert 'rollback keeps prior recipe' ($beforeJ -eq $afterJ) "before=[$($beforeJ -replace "`n",',')] after=[$($afterJ -replace "`n",',')]"

# 9) 后端对未知 /api 路径返回 200+空列表（全局兜底行为，实测记录）。
#    这正是旧 DishCost.vue 静默空列表的土壤；零 /dish-cost/* 请求由浏览器网络断言证明（见覆盖范围文档）。
$unknown = Invoke-RestMethod -Uri "$ApiBase/api/definitely-not-real" -Headers $H
Assert 'unknown-path fallback 200 empty (documented behavior)' ($unknown.code -eq 200 -and $unknown.data.Count -eq 0) "code=$($unknown.code) dataCount=$($unknown.data.Count)"

Write-Output "=== RESULT: PASS=$($script:pass) FAIL=$($script:fail) ==="
if ($script:fail -eq 0) { exit 0 } else { exit 1 }
