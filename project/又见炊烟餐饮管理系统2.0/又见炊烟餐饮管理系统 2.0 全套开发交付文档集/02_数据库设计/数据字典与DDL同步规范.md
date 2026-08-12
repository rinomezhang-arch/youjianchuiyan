# 又见炊烟餐饮管理系统 2.0 - 数据字典与 DDL 同步规范

> 版本：V2.0
> 创建日期：2026-08-02
> 维护：Trae（TRAE-BOT, trae@project.local）
> 适用范围：又见炊烟餐饮管理系统 2.0 全部数据库变更（78 张业务表 + 7 张 RBAC 表）
> 配套文档：《数据库设计说明书V2.0》《完整数据字典.md》《事务并发锁数据删除规范.md》《Git 分支管理规范.md》

---

## 文档目的

本规范约束"又见炊烟餐饮管理系统 2.0"项目所有数据库结构变更（DDL）的管理流程，确保 DDL 与数据字典、代码、文档三方一致，实现"DDL 即文档、字典可追溯、变更可回滚"。项目当前包含 78 张业务表与 7 张 RBAC 表，采用 MySQL 8.0 + 多租户 store_id 隔离架构，所有 DDL 变更必须通过 Flyway 迁移脚本 + CHANGELOG 双记录机制管理，禁止任何角色直接在生产数据库执行手工 DDL。

---

## 一、数据字典管理原则

### 1.1 三大原则

| 原则 | 含义 | 落地措施 |
|------|------|---------|
| DDL 即文档 | 数据库结构本身即权威文档，不维护独立 Word 字典 | Flyway 脚本即变更历史，information_schema 自动生成字典 |
| 字典与代码双向校验 | 字典字段与实体类字段必须一致 | CI 跑字典生成脚本 + 实体字段对比 |
| 版本化追溯 | 任意时点的库结构可重建 | Flyway 版本号 + CHANGELOG 条目 + Git 标签 |

### 1.2 字典权威来源

- **结构定义**：Flyway 迁移脚本（`db/migration/V*.sql`）为唯一权威。
- **运行时快照**：`information_schema.COLUMNS` 查询结果为当前库结构快照。
- **文档输出**：`完整数据字典.md` 由 `gen_data_dictionary.py` 自动生成，禁止手工编辑。
- **变更记录**：`CHANGELOG.md` 记录每次变更的人工说明。

### 1.3 字典字段标准

数据字典文档每个字段必须包含以下信息：

| 字段 | 说明 | 来源 |
|------|------|------|
| 表名 | 物理表名 | information_schema.TABLES.TABLE_NAME |
| 表注释 | 业务含义 | TABLES.TABLE_COMMENT |
| 字段名 | 物理字段名 | COLUMNS.COLUMN_NAME |
| 数据类型 | 含长度 | COLUMN_TYPE |
| 允许空 | YES/NO | IS_NULLABLE |
| 默认值 | 默认值 | COLUMN_DEFAULT |
| 字符集 | 字段字符集 | CHARACTER_SET_NAME |
| 索引 | 索引类型与名称 | STATISTICS |
| 外键 | 外键约束 | KEY_COLUMN_USAGE |
| 字段注释 | 业务含义 | COLUMN_COMMENT |

---

## 二、Flyway 迁移脚本规范（重点）

### 2.1 目录结构

```
banquet_project/
  src/main/resources/
    db/
      migration/
        V202608021500__add_reimbursement_table.sql     # 正向迁移
        V202608031000__add_store_id_to_member.sql
        U202608021500__add_reimbursement_table.sql     # 回滚脚本（可选）
        R__repeatable_view_kds_dashboard.sql           # 可重复执行视图
```

### 2.2 命名规则

| 脚本类型 | 命名格式 | 示例 | 说明 |
|---------|---------|------|------|
| 版本迁移 | V{yyyy}{MM}{dd}{HHmm}__{描述}.sql | V202608021500__add_reimbursement_table.sql | 一次性执行 |
| 回滚脚本 | U{yyyy}{MM}{dd}{HHmm}__{描述}.sql | U202608021500__add_reimbursement_table.sql | 手动执行回滚 |
| 可重复脚本 | R__{描述}.sql | R__repeatable_view_kds_dashboard.sql | checksum 变化时重跑 |

### 2.3 版本号递增规则

```text
- 版本号 = 提交时间戳（yyyyMMDDHHmm），精确到分钟
- 同一分钟内不得提交两个迁移脚本（避免版本号冲突）
- 版本号必须严格递增，CI 校验新脚本版本号 > 已合并最大版本号
- 描述部分使用小写英文 + 下划线，语义化表达变更内容
- 禁止：V1 / V2 / V001 等简短编号（必须用时间戳）
```

### 2.4 不可变原则

- 已合并并执行的迁移脚本**禁止修改**（哪怕只是改注释），只能新增脚本修正。
- Flyway 通过 checksum 校验脚本完整性，修改已执行脚本会导致启动失败。
- 如需修改，必须新增一个 `V{新时间戳}__fix_xxx.sql` 脚本。
- CI 校验：已合并的 V 脚本 git diff 必须为空，否则 PR 拒绝。

### 2.5 失败处理：flyway_schema_history 修复流程

```text
迁移失败处理流程：
  1. Flyway 执行失败时，数据库会留下 success=0 的记录
  2. 人工登录数据库，手动修复失败的 SQL（如语法错误）
  3. 删除 flyway_schema_history 中失败的记录：
     DELETE FROM flyway_schema_history WHERE success = 0;
  4. 重新启动应用，Flyway 重新执行修复后的脚本
  5. 在 CHANGELOG.md 记录失败原因与修复过程

禁止：
  - 直接在 flyway_schema_history 表手工插入成功记录（掩盖问题）
  - 删除整个 flyway_schema_history 表（丢失变更历史）
```

### 2.6 迁移脚本编写规范

```sql
-- V202608021500__add_reimbursement_table.sql
-- 功能：新增报销单表
-- 作者：DL-BOT
-- 关联 PR：#102
-- 回滚脚本：U202608021500__add_reimbursement_table.sql

CREATE TABLE reimbursement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    store_id BIGINT NOT NULL COMMENT '门店ID（多租户隔离）',
    reimbursement_no VARCHAR(32) NOT NULL COMMENT '报销单号',
    amount DECIMAL(12,2) NOT NULL COMMENT '报销金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待审 1已审 2已驳',
    apply_user_id BIGINT NOT NULL COMMENT '申请人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_no (store_id, reimbursement_no),
    KEY idx_store_status (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销单表';
```

编写要求：
- 每个字段必须有 COMMENT 注释。
- 业务表必须包含 `store_id` 字段 + 索引（见第八章）。
- 必须显式声明 ENGINE、CHARSET、COMMENT。
- 禁止使用外键约束（应用层保证一致性）。
- 字段命名蛇形，表名单数。
- 时间字段统一 DATETIME，金额统一 DECIMAL(12,2)。

---

## 三、CHANGELOG 双记录机制（重点）

### 3.1 双记录要求

每次数据库变更必须同时记录以下两份，缺一不可，CI 校验未记录 CHANGELOG 的 DDL PR 拒绝合并。

| 记录方式 | 性质 | 位置 | 内容 |
|---------|------|------|------|
| Flyway 迁移脚本 | 自动执行 | `db/migration/V*.sql` | 可执行的 DDL 语句 |
| CHANGELOG 条目 | 人工记录 | `CHANGELOG.md` | 变更原因、影响、回滚、责任人、PR |

### 3.2 CHANGELOG.md 格式（Keep a Changelog 1.0.0）

项目根目录 `CHANGELOG.md` 遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 规范，数据库变更单独章节记录。

```markdown
## [DB-2026-08-02-01] - 2026-08-02

### Added - 数据库变更
- 新增 reimbursement 报销单表
  - Flyway: V202608021500__add_reimbursement_table.sql
  - 变更原因：财务模块新增报销单业务，需独立表存储
  - 影响范围：财务模块、审批流模块
  - 回滚脚本：U202608021500__add_reimbursement_table.sql
  - 回滚预案：DROP TABLE reimbursement（无业务数据可丢弃）
  - 责任人：DL-BOT
  - 关联 PR：#102
  - 多租户：已包含 store_id 字段 + 联合索引
```

### 3.3 示例条目（至少 3 个）

```markdown
## [DB-2026-08-03-01] - 2026-08-03

### Changed - 数据库变更
- 修改 member 表新增 address_encrypted 加密字段
  - Flyway: V202608031000__add_encrypted_address_to_member.sql
  - 变更原因：会员地址需 AES-256 加密存储，满足 PIPL 合规
  - 影响范围：会员模块、营销模块
  - 回滚脚本：U202608031000__add_encrypted_address_to_member.sql
  - 回滚预案：数据迁移完成后 DROP COLUMN（迁移前不可回滚）
  - 责任人：DL-BOT
  - 关联 PR：#105
  - 数据迁移：旧 address 字段加密后写入新字段，校验通过后清空旧字段
```

```markdown
## [DB-2026-08-05-01] - 2026-08-05

### Fixed - 数据库变更
- 修复 order 表缺少 store_id+status 联合索引导致查询慢
  - Flyway: V202608051430__add_index_order_store_status.sql
  - 变更原因：订单列表按门店+状态查询无索引，P95 延迟 > 2s
  - 影响范围：订单模块、KDS 模块、报表模块
  - 回滚脚本：U202608051430__add_index_order_store_status.sql
  - 回滚预案：DROP INDEX idx_store_status
  - 责任人：DL-BOT
  - 关联 PR：#108
  - 性能预估：索引新增后 P95 预期 < 100ms，写入性能下降 < 5%
```

```markdown
## [DB-2026-08-10-01] - 2026-08-10

### Removed - 数据库变更
- 删除废弃表 temp_import_member（历史数据导入临时表）
  - Flyway: V202608101200__drop_temp_import_member_table.sql
  - 变更原因：数据迁移已完成，临时表不再使用，占用空间
  - 影响范围：无（表已无业务引用）
  - 回滚脚本：U202608101200__drop_temp_import_member_table.sql（重建空表）
  - 回滚预案：从备份恢复表结构（数据不恢复）
  - 责任人：rinomezhang-arch
  - 关联 PR：#115
  - 风险评估：已扫描全代码无引用，已备份表结构与样本数据
```

---

## 四、数据字典自动生成机制

### 4.1 自动生成流程

```text
生成流程：
  1. 开发者提交含 DDL 的 PR
  2. CI 流水线执行 Flyway 迁移至临时库
  3. CI 执行 gen_data_dictionary.py 脚本
  4. 脚本查询 information_schema.COLUMNS 生成字典 md
  5. CI 比对新生成的字典与仓库版本，差异作为 PR 检查项
  6. PR 合并后字典 md 自动更新至仓库
```

### 4.2 gen_data_dictionary.py 脚本规范

```python
# gen_data_dictionary.py 核心逻辑（伪代码）
import pymysql
import os

def gen_dictionary(host, db, output_path):
    conn = pymysql.connect(host=host, database=db, user=os.getenv('DB_USER'),
                           password=os.getenv('DB_PWD'), charset='utf8mb4')
    cursor = conn.cursor()

    # 查询所有表
    cursor.execute("""
        SELECT TABLE_NAME, TABLE_COMMENT, ENGINE
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = %s
        ORDER BY TABLE_NAME
    """, (db,))
    tables = cursor.fetchall()

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('# 又见炊烟餐饮管理系统 2.0 - 完整数据字典（自动生成）\n\n')
        f.write('> 本文件由 gen_data_dictionary.py 自动生成，禁止手工编辑\n')
        f.write('> 生成时间：{}\n\n'.format(datetime.now()))

        for table_name, table_comment, engine in tables:
            f.write('## {} - {}\n\n'.format(table_name, table_comment))
            f.write('| 字段名 | 数据类型 | 允许空 | 默认值 | 字符集 | 索引 | 注释 |\n')
            f.write('|--------|---------|--------|--------|--------|------|------|\n')
            # 查询字段、索引
            cursor.execute("""
                SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
                       CHARACTER_SET_NAME, COLUMN_COMMENT
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
                ORDER BY ORDINAL_POSITION
            """, (db, table_name))
            for col in cursor.fetchall():
                indexes = get_indexes(cursor, db, table_name, col[0])
                f.write('| {} | {} | {} | {} | {} | {} | {} |\n'.format(*col, indexes))
            f.write('\n')
```

### 4.3 字典校验项

CI 在生成字典后执行以下校验，任一失败则 PR 拒绝：

| 校验项 | 规则 | 失败处理 |
|--------|------|---------|
| 表注释非空 | TABLE_COMMENT 不得为空 | 拒绝 |
| 字段注释非空 | 每个字段 COLUMN_COMMENT 不得为空 | 拒绝 |
| 业务表含 store_id | 业务表必须有 store_id 字段 | 拒绝 |
| 主键存在 | 每张表必须有主键 | 拒绝 |
| 字符集统一 | utf8mb4 | 拒绝 |
| 存储引擎统一 | InnoDB | 拒绝 |
| 字典 md 已更新 | PR 含字典 md 变更 | 拒绝 |

---

## 五、数据库变更评审流程

### 5.1 评审流程

```text
DDL 变更评审流程：
  1. 提交者在 feat-* 分支编写 Flyway 脚本 + CHANGELOG 条目
  2. 提交者填写《DDL 评审表》（见 5.2）作为 PR 描述
  3. 发起 PR 至 develop，CI 自动跑迁移与字典校验
  4. 至少 2 名 Reviewer 审核（其中 1 名必须为人工）
  5. 涉及支付/会员/订单核心表的变更，需架构师额外签字
  6. 审核通过后合并，CI 自动执行 Flyway 至开发库
  7. 发布到测试库、预发库、生产库分别由对应流水线触发
  8. 通知企业微信群变更已生效
```

### 5.2 DDL 评审表（PR 必填）

| 字段 | 说明 |
|------|------|
| 变更类型 | 新增表 / 修改字段 / 新增索引 / 删除表 |
| 影响范围 | 受影响的模块、接口、表 |
| 兼容性 | 向前兼容 / 向后兼容 / 不兼容（需停机） |
| 回滚方案 | 回滚脚本路径 + 回滚预案 |
| 性能预估 | 索引新增预估写入影响 / 大表加索引是否走 pt-osc |
| 数据迁移 | 是否涉及数据迁移，迁移脚本路径 |
| 多租户影响 | 是否影响 store_id 隔离 |
| 停机要求 | 是否需要停机维护，停机窗口 |
| 备份要求 | 是否需要前置备份，备份策略 |

### 5.3 审核标准

- 必须包含 Flyway 脚本 + 回滚脚本 + CHANGELOG 条目（三件套）。
- 必须通过 CI 全部校验（编译、字典生成、字段注释、store_id 校验）。
- 至少 2 人审核通过，其中 1 名人工。
- 大表（> 100 万行）加索引必须走 pt-online-schema-change，禁止直接 ALTER。
- 删除字段必须先标记 deprecated 一个版本，下个版本再删除。

---

## 六、紧急 DDL 处理（线上故障修复）

### 6.1 紧急流程

线上故障（如索引缺失导致查询超时、字段缺失导致接口报错）允许跳过完整评审流程，但必须事后补齐 CHANGELOG 与评审记录。

```text
紧急 DDL 流程：
  1. 故障发生，oncall 工程师评估需紧急 DDL
  2. 老板（boss）口头或企业微信授权
  3. 在 hotfix-* 分支编写 Flyway 脚本
  4. 跳过评审，人工直接执行 SQL（生产库）+ 提交脚本到仓库
  5. 故障恢复后 24 小时内补 CHANGELOG 条目 + 补评审记录
  6. 复盘报告中说明跳过评审的合理性
```

### 6.2 紧急 DDL 限制

- 仅限 P0/P1 故障可走紧急流程，P2/P3 必须走完整流程。
- 紧急 DDL 必须先备份数据库（mysqldump 或快照）。
- 紧急 DDL 仍需编写 Flyway 脚本（事后补提交），保持迁移历史完整。
- 紧急 DDL 不得删除表或字段（只能新增索引、新增字段）。
- 紧急 DDL 执行后必须验证，验证失败立即回滚。

---

## 七、数据库版本与代码版本对应关系

### 7.1 版本对应要求

每个代码发布版本必须明确对应的数据库版本，Release Notes 必须包含 DB 版本号。

| 代码版本 | DB 版本 | 对应 Flyway 最大版本号 | Release Notes |
|---------|---------|----------------------|--------------|
| v2.0.0 | DB-2.0.0 | V202608021500 | 含 DB-2.0.0 升级说明 |
| v2.0.1 | DB-2.0.1 | V202608051430 | 含 DB-2.0.1 升级说明 |

### 7.2 升级顺序

```text
升级部署顺序（禁止颠倒）：
  1. 备份数据库
  2. 停止应用服务
  3. 执行 Flyway 迁移（应用启动时自动执行）
  4. 验证数据库结构（字典校验脚本）
  5. 启动新版本应用
  6. 冒烟测试核心功能
  7. 放流量

降级顺序（不兼容降级需特殊处理）：
  1. 停止应用
  2. 执行回滚脚本 U*.sql（如有）
  3. 启动旧版本应用
  4. 验证

禁止：
  - 先启动应用再跑 Flyway（应用可能因字段缺失启动失败）
  - 降级时不执行回滚脚本（结构不一致导致数据错乱）
```

### 7.3 版本兼容矩阵

Release Notes 必须明确标注升级兼容性：

| 兼容级别 | 含义 | 升级要求 |
|---------|------|---------|
| 向前兼容 | 新代码可读旧库结构 | 仅新增字段/索引，可直接升级 |
| 向后兼容 | 旧代码可读新库结构 | 删除字段需 deprecated 过渡 |
| 不兼容 | 必须停机升级 | 标注停机窗口与回滚预案 |

---

## 八、多租户 DDL 注意事项

### 8.1 store_id 强制要求

项目采用多租户 store_id 隔离（宁国店=1、宣城店=2），任何业务表新增必须遵循以下规则：

```text
多租户 DDL 强制规则：
  1. 新增业务表必须包含 store_id BIGINT NOT NULL 字段
  2. store_id 必须有索引（单列索引或联合索引的首列）
  3. 唯一索引必须包含 store_id（如 uk_store_no(store_id, no)）
  4. 查询 SQL 必须带 store_id 条件（MyBatis 拦截器自动注入）
  5. RBAC 表（7 张）不含 store_id（系统级表）
```

### 8.2 多租户 DDL 示例

```sql
-- 正确：业务表含 store_id
CREATE TABLE banquet_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL COMMENT '门店ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_no (store_id, order_no),    -- 唯一索引含 store_id
    KEY idx_store_status (store_id, status)          -- 查询索引含 store_id
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宴会订单表';

-- 错误（CI 拒绝）
CREATE TABLE banquet_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_no (order_no)   -- 缺少 store_id，跨租户数据冲突
);
```

### 8.3 多租户校验清单

CI 对每个含 DDL 的 PR 执行以下校验：

| 校验项 | 规则 | 失败处理 |
|--------|------|---------|
| 业务表含 store_id | 新增业务表必须有 store_id 字段 | 拒绝 |
| store_id NOT NULL | store_id 不得允许空 | 拒绝 |
| 唯一索引含 store_id | 唯一索引首列必须为 store_id | 拒绝 |
| 查询索引含 store_id | 至少一个索引首列为 store_id | 拒绝 |
| RBAC 表识别 | 7 张 RBAC 表白名单豁免 store_id 校验 | 通过 |

### 8.4 多租户数据迁移注意

- 跨门店数据迁移必须显式指定 store_id，禁止依赖默认值。
- 历史数据补 store_id 时必须按门店分批执行，避免大事务锁表。
- 多租户字段加密密钥按门店隔离（store_id 作为密钥派生因子之一）。

---

## 附则

- 本规范由 Trae（TRAE-BOT）维护，修订需提 PR 经人工审核。
- 本规范与《数据库设计说明书V2.0》《完整数据字典.md》《事务并发锁数据删除规范.md》配套使用，DDL 管理以本文件为准。
- 本规范所有 SQL 示例为说明性内容，不直接修改任何 .java 源文件，落地由开发分支实现。
- 本规范自 2026-08-02 起生效，适用于又见炊烟餐饮管理系统 2.0 全部 78 张业务表 + 7 张 RBAC 表。
