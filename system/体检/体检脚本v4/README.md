# 体检脚本说明文档

> 最后更新: 2026-08-01 21:00

## 📌 推荐使用（v4 模块化版本）

一键扫描，输出 5 种格式报告到 `reports/` 目录。

```bash
python checkup.py
```

### 模块文件

| 文件 | 用途 | 说明 |
|------|------|------|
| `checkup.py` | **一键入口** | 运行这个就行 |
| `config.py` | 配置加载 | 从 `.env` 读取数据库配置 |
| `db.py` | 数据库连接 | pymysql 封装，支持真实 FK 查询 |
| `analyzer.py` | 分析引擎 | 9 大维度分析（主键/外键/索引/空表/字符集/引擎/冗余/多门店/注释） |
| `report.py` | 报告生成 | HTML/MD/JSON/CSV/TXT 五格式 |
| `.env` | 配置文件 | 数据库连接信息 |

### 输出文件（在 `reports/` 目录）

| 文件 | 格式 | 用途 |
|------|------|------|
| `report_*.html` | HTML | 交互式报告，浏览器打开 |
| `report_latest.html` | HTML | 最新副本，方便直接打开 |
| `report_*.md` | Markdown | 文本报告，适合文档归档 |
| `data_*.json` | JSON | 结构化数据，供程序读取 |
| `findings_*.csv` | CSV | 表格数据，Excel 打开 |
| `summary_*.txt` | TXT | 纯文本摘要 |

---

## 🗂️ 历史版本（可归档）

这些是重构前的旧版本，功能已被 v4 替代。

| 文件 | 版本 | 说明 | 状态 |
|------|------|------|------|
| `system_checkup_v3.py` | v3 | 单文件版本，subprocess 调 mysql | ⚠️ 已废弃 |
| `scan_all_system_v2.py` | v2 | 175KB 超大单文件，含 Entity 分析 | ⚠️ 已废弃 |
| `deep_scan.py` | - | 深度扫描，依赖 Java Entity 代码 | ❌ 不适用（Entity 与 DB 不同步） |
| `scan_alignment.py` | - | 对齐扫描 | ⚠️ 已废弃 |
| `gen_comparison.py` | - | 对比报告生成 | ⚠️ 已废弃 |
| `gen_detail_report.py` | - | 详细报告生成 | ⚠️ 已废弃 |
| `extract_audit.py` | - | 提取审计数据 | ⚠️ 已废弃 |
| `dump_findings.py` | - | 导出发现项 | ⚠️ 已废弃 |

---

## 📊 数据文件（可定期清理）

这些是历史扫描产生的数据，可归档或删除。

| 文件模式 | 说明 |
|----------|------|
| `audit_data*.json` | 审计数据（v2/v3 历史） |
| `audit_data_v2.js` | v2 数据（JS 格式，用于 HTML） |
| `alignment_scan.json` | 对齐扫描数据 |
| `deep_alignment_scan.json` | 深度对齐数据 |
| `system_checkup_v*.html` | 历史 HTML 报告 |
| `orphan_fix_comparison.html` | 孤儿记录修复对比 |

---

## 🔧 依赖

```bash
pip install pymysql
```

---

## 📝 使用方法

### 1. 配置数据库（首次使用）

编辑 `.env` 文件：

```ini
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASS=你的密码
DB_NAME=banquet
```

### 2. 运行体检

```bash
python checkup.py
```

### 3. 查看报告

打开 `reports/report_latest.html`

---

## 🎯 分析维度（9 项）

1. **主键分析** — 检查无主键表、varchar/int 主键风险
2. **外键分析** — 真实 FK 约束 + _id 后缀推断，检查类型匹配和孤儿记录
3. **索引分析** — store_id 索引覆盖
4. **空表检测** — 区分业务表和配置表
5. **字符集一致性** — 排序规则统一
6. **引擎检查** — 非 InnoDB 表
7. **字段冗余** — price/cost_price、name 冗余
8. **多门店隔离** — 业务表缺 store_id
9. **注释完整性** — 表/列注释缺失

---

## 📈 版本演进

| 版本 | 特点 | 问题 |
|------|------|------|
| v2 | 单文件 175KB，含 Entity 分析 | 代码臃肿，Entity 与 DB 不同步导致误报 |
| v3 | 纯 DB 分析，subprocess 调 mysql | 密码暴露，单文件，无分页 |
| **v4** | 模块化，pymysql，多格式报告 | ✅ 当前推荐 |

---

## 🗑️ 清理建议

历史文件可移动到 `体检/archive/` 或直接删除：

```bash
# 移动到归档目录
mkdir archive
move system_checkup_v3.py archive/
move scan_all_system_v2.py archive/
move deep_scan.py archive/
move *.json archive/
```

---

**维护者**: 地龙 🐉  
**项目**: 又见炊烟餐饮管理系统
