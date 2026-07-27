# 数据库变更日志 — 餐饮管理系统

## 🔒 变更铁律（秋哥钦定 2026-07-22）
1. **发现问题** → 第一时间运行测试，找到根因
2. **获得容许** → 必须经秋哥同意后才能改
3. **执行变更** → DDL/DML操作
4. **更新文档** → 更新数据库说明文件
5. **写入日志** → 变更内容写入本文件
6. **三龙同步** → 通知天龙/地龙/SOLO

---

## 变更记录

### 2026-07-22 20:27 — 天龙🦞
**操作**: 新建表 `booking_dish`
**原因**: P0点菜功能需要预订-菜品关联表
**DDL**:
```sql
CREATE TABLE booking_dish (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id VARCHAR(20) NOT NULL,
  dish_id VARCHAR(20) NOT NULL,
  dish_name VARCHAR(100),
  price DECIMAL(10,2),
  quantity INT DEFAULT 1,
  remark VARCHAR(200),
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_booking (booking_id),
  INDEX idx_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
**影响**: 仅新增表，不影响已有51表
**批准**: 秋哥

---

### 2026-07-22 20:54-20:59 — 天龙🦞
**操作**: Java代码SQL列名修复（不改DB表结构）
**原因**: 新Controller中SQL列名与DB真实列名不匹配
**修复明细**:

| 文件 | 错误列名 | 正确列名 |
|------|---------|---------|
| BookingExtController.java | people_count | guest_count |
| BookingExtController.java | banquet_type | banquet_name |
| BookingExtController.java | status | booking_status |
| BookingExtController.java | booking_period | booking_time |
| DashboardReportV2Controller.java | people_count | guest_count |
| DashboardReportV2Controller.java | booking_period | booking_time |
| DashboardReportV2Controller.java | bt.table_area | tm.table_area (JOIN) |

**影响**: 代码适配DB，DB表结构不变

---

## 当前DB状态
- **快照文件**: 数据库基准快照.txt
- **表数**: 51
- **字段数**: 572
- **最后更新时间**: 2026-07-22 20:59

---
🦞 建立于 2026-07-22
