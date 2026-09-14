# TR62 备份、迁移预检、回退与发布后核验步骤

执行人：Trae。日期：2026-09-14。本文档只给步骤与责任人；实际发布须等 Codex 发布令，本卡未授权任何生产写入或重启。

## 一、发布前备份（全部留 sha256 与时间戳）

1. 数据库备份（生产 banquet 库）：
   `mysqldump --single-transaction --routines --triggers banquet | gzip > banquet_pre_<ts>.sql.gz`
   记录行数、文件大小、sha256，上传 COS 备份目录（主桶 youjian-data-1409286104 既定备份路径）。
2. 源码树备份：`tar czf banquet_project_pre_<ts>.tar.gz ~/deploy_tmp_main/banquet_project`
   （整体归档含未提交改动与法务目录；归档只为回退，不解读法务内容。）记录 sha256 并上传 COS。
3. 运行 JAR 备份：`cp target/banquet-1.0.0.jar target/banquet-1.0.0.jar.pre_<ts>`，
   记录 sha256（当前值 2a9e033582963d62d1c6c227635b488303f63d7f1bb896d8271cc580d3544785）。
4. 前端 dist 备份：`cp -r /opt/youjianchuiyan/frontend_v3/dist /opt/youjianchuiyan/frontend_v3/dist.pre_<ts>`。
5. 以上四份的 sha256 清单落登记簿与发布记录。

## 二、迁移预检

1. 列出候选 scripts/migrations/ 全部迁移与生产库已应用清单对账，产出"待执行迁移列表"。
2. 先在隔离 schema（非生产端口 MySQL）对生产结构副本演练全部待执行迁移，验证幂等与回退语句。
3. 重点核对已 reviewed 的迁移是否已上过生产（如 stocktake_precision_v1：stock_take_detail DECIMAL(12,3)/unit_price DECIMAL(16,8)）。
4. 预检不通过：停止发布，阻断上板回 codex/current。

## 三、发布执行（须持 Codex 发布令，以下为既定三铁律流程）

1. 在服务器上 `mvn package`（禁止本地打包传 jar）；打包前同步 src/main/resources/ 全部资源。
2. 打包后必验 `unzip -l target/*.jar | grep legal/`，确认 case_rules.txt、case_dossier.txt 等在 jar 内。
3. 法务文件不回拷覆盖：LegalController/LegalEvidenceService/LegalRetrievalService/LegalCosConfig
   与 resources/legal/ 保持线上版（hash 见 candidate-manifest.md 第四节）。
4. 线上独有 LoginCredential.java、RoleScopeInterceptor.java 及 5 项未提交改动的并入方式，
   以 Codex 裁决后的最终源码树为准；裁决未完成不得发布。
5. systemctl stop banquet → 换 jar → systemctl start banquet → 立即健康检查。
6. application-prod.yml 永不覆盖。

## 四、回退

1. 应用回退：systemctl stop banquet → 恢复 banquet-1.0.0.jar.pre_<ts> → start → 健康检查。
2. 源码回退：以第一步源码归档整树恢复（含法务与未提交改动）。
3. 前端回退：dist.pre_<ts> 整目录恢复。
4. 数据库回退：仅当迁移已造成不可逆影响时，以 banquet_pre_<ts>.sql.gz 恢复；
   恢复属重大操作，须秋哥本人批准（红线：生产数据删除/覆盖）。
5. 回退完成后登记实际执行项与证据。

## 五、发布后核验（全部真实点击/真实接口，不以页面 200 充数）

1. 健康端点与 banquet 服务 active。
2. 三账号黑盒：rino、zhangjing 登录→各自权限界面→退出；lawyer 登录→整页落 /case/。
3. 五链冒烟（合成测试数据，带标识可追踪）：采购入库一单、盘点一单（含小数精度）、
   工资审批付款一笔、收款流水一笔（含应收联动）、公开 H5 咨询一条（含回查）。
4. 法务 /case/ 可达，张律师视角仅见法务。
5. 双店隔离抽查：经理跨店访问被拒。
6. 核验结果连同 sha256 清单回任务板，未通过项立即 blocked 并回 codex/current。
