# TR-RECEIPT-REAL-24 r3 验收报告

## 任务ID
TR-RECEIPT-REAL-24

## HEAD
- 工作树 `trae-receipt-real-24` HEAD = `4c06b9100b520cbadca7b3ea1f37ef4d93255110`
- 源码提交 `52f5df62`（frontend_v3 + banquet_project 与 HEAD 零差异，git diff --quiet 确认）
- dist 336 文件从 52f5df62 源码 + 锁定依赖（node_modules + package-lock.json）重建

## 制品 SHA
- **distHash** = `b2853bd67c8221d8838c587434d9667376692770049cce95f37cc9fe5f4f0b6b`（PowerShell Get-FileHash 大写，runner manifest gate OK）
- **sourceHead** = `52f5df62`
- **distFileCount** = 336
- **唯一 run 路径** = `docs/协作/Trae/receipt-real-24/evidence/runs/20260913-155702-7ae3/`
- PDF sha256 = 50240 字节；preview png = 28170 字节

## 真实数字
- **pass=32 fail=0 skip=1**（skip=物理打印机出纸，环境无实体打印机）
- 后端定向测试 BillReceiptTest：14 pass / 0 fail / 0 error / 0 skip（mvn -o -q -Dtest=BillReceiptTest test）
- Runner 全链退出码=0（TR24_RUNNER_OK）

## 关键验证项
- 4 合成账号真实登录取 JWT
- 无 JWT=401；GM 缺/0/all/非法 storeId=400；未知订单=404；跨店=403
- GM 返回真实小票：2 道菜、total=100、final=100、应付金额表述
- 桌台贯通：JSON 聚合 `TR24-01号桌、TR24-02号桌`，按 table_booking_id 稳定排序
- 实际点击业务打印按钮 → window.print 插桩 printCalls=1
- @page { size: 80mm auto } → PDF MediaBox widthPt=227.04 ≈ 80mm(226.77pt)
- XSS 菜名原样显示无注入；弹窗拦截/后端错误无成功提示
- 打印链只读：前后订单/明细行数不变 [2,2]→[2,2]，6 项孤儿/跨店计数全 0

## 修复内容
- manifest distHash 恢复为 `b2853bd6…`（原值正确；此前 Python 小写 hex 误判为不一致是计算方法差异，非 dist 本身问题）
- dist 从 52f5df62 源码重建确认（vite build 336 文件，内容与原 dist 一致）
- build-manifest.json 更新 builtAt/rebuiltAt/runPath 字段
- 隔离库 co_print23_20260909_022305 因机器重启丢失，从原始 setup-isolated-schema.ps1 恢复（CREATE DATABASE IF NOT EXISTS + CREATE TABLE IF NOT EXISTS，仅 INSERT 合成数据，不碰共享 banquet 库）

## 未验门槛
- 物理打印机出纸（环境无实体打印机/网络打印服务，诚实标为 skip）
- 生产上线（法务冻结，不碰生产）
- 工资/iPad/RC 大矩阵（未跑，按任务要求禁止）

## 禁止项确认
- 未重跑工资、iPad、RC 大矩阵
- 未连接生产、未碰法务
- 未改全局 request 拦截器/鉴权/环境配置
- 未新建数据库（仅恢复已存在的隔离 schema）
