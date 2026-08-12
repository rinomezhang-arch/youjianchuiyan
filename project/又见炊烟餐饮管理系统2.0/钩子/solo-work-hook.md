# solo — 秋哥钦定工作钩子
# 挂载日期：2026-08-03
# 此文件由地龙 🐉 代秋哥设定，solo每次干活前自动加载

## 🚨 工作目录
F:\solo\project\又见炊烟餐饮管理系统2.0

## 🚨 规范文档总入口
F:\solo\project\又见炊烟餐饮管理系统2.0\又见炊烟餐饮管理系统 2.0 全套开发交付文档集\

## 🚨 数据库连接
- Docker MySQL: 127.0.0.1:3307 (用户 rino, 密码 Wo002323, 库 banquet)
- 本地 MySQL: 127.0.0.1:3306

## 🚨 铁律（违反一条从头重做）

### 数据完整性
1. 所有表和字段必须有中文COMMENT注释
2. NOT NULL + 无默认值的字段必须填值
3. 计算列（CALC）不准直接填数值，必须用SELECT/公式推导
4. 外键必须有效，不能指向不存在的ID

### 数据流向
5. 没供应商 → 不能采购
6. 没采购 → 不能入库
7. 没入库 → 不能建原料档案
8. 没原料 → 不能做成本换算
9. 没成本换算 → 不能建菜品配方
10. 没配方 → 不能建成本卡
11. 没成本卡 → 不能入库菜品
12. 跳过任何一步 = 全链路作废

### 计算公式
13. net_price_per_unit = purchase_price / conversion_rate / (yield_rate / 100)
14. purchase_order_detail.amount = quantity × unit_price
15. purchase_receipt_detail.amount = actual_quantity × unit_price
16. purchase_order.total_quantity = SUM(purchase_order_detail.quantity)
17. dish_cost_card.standard_cost = SUM(dish_recipe.total_cost)
18. dish_master.cost_price = dish_cost_card.standard_cost（不准抄Excel！）
19. dish_master.cost_rate = cost_price / sale_price × 100

### 单位换算（从unit_conversion表查，不准手填）
20. 斤 → 克 = 500
21. 公斤 → 克 = 1000
22. 箱 → 克 = 按规格计算
23. 瓶 → 克 = 按容量（500ml≈500g）
24. 只/个 → 克 = 按单重

### 出成率（不能全填100%）
25. 畜肉类 80-95%、禽类 70-85%、水产 65-80%、蔬菜 80-90%
26. 调味品/粮油/酒水 = 100%

### 代码规范
27. 写SQL文件不算完，必须在数据库里执行通过
28. 执行完必须跑检测脚本验证
29. 不准DROP TABLE（除非秋哥明确确认）
30. 不准DELETE FROM（除非秋哥明确确认）

## 🚨 检测脚本
每次完成任务后立即执行：
```
cmd /c "mysql -u rino -pWo002323 -h 127.0.0.1 -P 3307 banquet --default-character-set=utf8mb4 < F:\solo\project\又见炊烟餐饮管理系统2.0\scripts\ultimate_checkup_v3.sql"
```

核心指标必须达到：
- net_price公式正确率 = 100%
- amount公式正确率 = 100%
- total_quantity = SUM(detail) = 100%
- cost_price回填一致率 = 100%
- 供应商FK断裂 = 0
- 原料supplier_id为空 = 0

## 🚨 秋哥给你的指令文件
- F:\solo\project\又见炊烟餐饮管理系统2.0\数据灌入计划-秋哥钦定.md
- F:\solo\project\又见炊烟餐饮管理系统2.0\字段规则-秋哥钦定.txt
- F:\solo\project\又见炊烟餐饮管理系统2.0\宴会预订+iPad点菜流程-建表灌数据.txt

## 🚨 违规记录
此文件每次启动自动加载。违反铁律的记录会写在这里：
（暂无）
