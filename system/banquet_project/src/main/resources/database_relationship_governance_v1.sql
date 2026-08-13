-- 数据库关系治理 v1
-- 原则：只为确定语义的父子关系建立外键；多态 business_id/related_id 不建立错误外键。
-- 事实表保持空表是合法状态，不制造交易、财务、客户或人事记录。

ALTER TABLE approval_flow
  ADD CONSTRAINT fk_approval_flow_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_approval_flow_applicant FOREIGN KEY (applicant_id) REFERENCES staff_master(staff_id) ON DELETE SET NULL;
ALTER TABLE approval_node
  ADD CONSTRAINT fk_approval_node_flow FOREIGN KEY (flow_id) REFERENCES approval_flow(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_approval_node_approver FOREIGN KEY (approver_id) REFERENCES staff_master(staff_id) ON DELETE SET NULL;

ALTER TABLE finance_voucher
  ADD CONSTRAINT fk_fin_voucher_store FOREIGN KEY (store_id) REFERENCES store_info(store_id);
ALTER TABLE finance_voucher_detail
  ADD CONSTRAINT fk_fin_voucher_detail_voucher FOREIGN KEY (voucher_id) REFERENCES finance_voucher(voucher_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_fin_voucher_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id);

ALTER TABLE member_level
  ADD CONSTRAINT fk_member_level_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT uk_member_level_store_code UNIQUE (store_id, level_code);
ALTER TABLE member_point_rule
  ADD CONSTRAINT fk_member_point_rule_store FOREIGN KEY (store_id) REFERENCES store_info(store_id);

ALTER TABLE purchase_order
  ADD CONSTRAINT fk_purchase_order_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master(supplier_id);
ALTER TABLE purchase_order_detail
  MODIFY ingredient_id varchar(50) NULL,
  ADD CONSTRAINT fk_purchase_order_detail_order FOREIGN KEY (order_id) REFERENCES purchase_order(order_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_purchase_order_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_order_detail_ingredient FOREIGN KEY (ingredient_id, store_id) REFERENCES ingredient_master(ingredient_id, store_id);

ALTER TABLE purchase_receipt
  ADD CONSTRAINT fk_purchase_receipt_order FOREIGN KEY (order_id) REFERENCES purchase_order(order_id),
  ADD CONSTRAINT fk_purchase_receipt_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master(supplier_id);
ALTER TABLE purchase_receipt_detail
  MODIFY ingredient_id varchar(50) NULL,
  ADD CONSTRAINT fk_purchase_receipt_detail_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_receipt(receipt_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_purchase_receipt_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_receipt_detail_ingredient FOREIGN KEY (ingredient_id, store_id) REFERENCES ingredient_master(ingredient_id, store_id);

ALTER TABLE purchase_return
  ADD CONSTRAINT fk_purchase_return_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_receipt(receipt_id),
  ADD CONSTRAINT fk_purchase_return_order FOREIGN KEY (order_id) REFERENCES purchase_order(order_id),
  ADD CONSTRAINT fk_purchase_return_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_return_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_master(supplier_id);
ALTER TABLE purchase_return_detail
  MODIFY ingredient_id varchar(50) NULL,
  ADD CONSTRAINT fk_purchase_return_detail_return FOREIGN KEY (return_id) REFERENCES purchase_return(return_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_purchase_return_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_purchase_return_detail_ingredient FOREIGN KEY (ingredient_id, store_id) REFERENCES ingredient_master(ingredient_id, store_id);

ALTER TABLE stock_take ADD CONSTRAINT fk_stock_take_store FOREIGN KEY (store_id) REFERENCES store_info(store_id);
ALTER TABLE stock_take_detail
  MODIFY ingredient_id varchar(50) NULL,
  ADD CONSTRAINT fk_stock_take_detail_take FOREIGN KEY (take_id) REFERENCES stock_take(take_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_stock_take_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_stock_take_detail_ingredient FOREIGN KEY (ingredient_id, store_id) REFERENCES ingredient_master(ingredient_id, store_id);
ALTER TABLE stock_loss ADD CONSTRAINT fk_stock_loss_store FOREIGN KEY (store_id) REFERENCES store_info(store_id);
ALTER TABLE stock_loss_detail
  MODIFY ingredient_id varchar(50) NULL,
  ADD CONSTRAINT fk_stock_loss_detail_loss FOREIGN KEY (loss_id) REFERENCES stock_loss(loss_id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_stock_loss_detail_store FOREIGN KEY (store_id) REFERENCES store_info(store_id),
  ADD CONSTRAINT fk_stock_loss_detail_ingredient FOREIGN KEY (ingredient_id, store_id) REFERENCES ingredient_master(ingredient_id, store_id);

-- 合法初始化：仅写入制度性主数据，不伪造消费、交易、财务或人事事实。
INSERT INTO member_level (store_id,level_code,level_name,min_points,min_recharge,discount_rate,point_rate,birthday_discount,benefits,is_active,sort_order)
SELECT s.store_id,'STANDARD','普通会员',0,0,1.00,1.00,0.95,'基础积分、生日礼遇',1,1 FROM store_info s
ON DUPLICATE KEY UPDATE level_name=VALUES(level_name),is_active=1;
INSERT INTO member_level (store_id,level_code,level_name,min_points,min_recharge,discount_rate,point_rate,birthday_discount,benefits,is_active,sort_order)
SELECT s.store_id,'VIP','贵宾会员',5000,5000,0.95,1.50,0.90,'积分加速、生日礼遇、优先预订',1,2 FROM store_info s
ON DUPLICATE KEY UPDATE level_name=VALUES(level_name),is_active=1;
INSERT INTO member_point_rule (store_id,rule_name,rule_type,point_value,amount_condition,is_active,description)
SELECT s.store_id,'消费积分','CONSUMPTION',1,1.00,1,'每实付1元积1分；退款时同步冲回' FROM store_info s
WHERE NOT EXISTS (SELECT 1 FROM member_point_rule r WHERE r.store_id=s.store_id AND r.rule_type='CONSUMPTION');
