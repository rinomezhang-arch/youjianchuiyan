-- 入库明细单价/金额精度加宽。GoodsReceiptItem 实体已按"最近一次入库价"口径改成
-- unit_price DECIMAL(15,8)、amount DECIMAL(15,4)，生产库仍是 (10,2)/(12,2)：
-- 单价被截成两位小数后，领料成本和配方标准成本会系统性偏低。
-- 加宽不丢历史值、不改行数，与 supply_price_precision_v1.sql 属同一批。
ALTER TABLE goods_receipt_item MODIFY COLUMN unit_price DECIMAL(15,8) NOT NULL;
ALTER TABLE goods_receipt_item MODIFY COLUMN amount DECIMAL(15,4) NOT NULL;
