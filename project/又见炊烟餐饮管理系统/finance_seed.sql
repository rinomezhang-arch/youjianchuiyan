-- ============================================================
-- 又见炊烟 - 财务11表真实业务数据造数
-- 覆盖时间：2026-04-01 至 2026-07-29（120天）
-- 双门店：1=宁国店（旗舰店），2=宣城店（分店）
-- 业务规则：
--   - 宴会定金 30% 现场收（payment_record）
--   - 宴会尾款 70% 宴会后 3 天结清（payment_record）
--   - 月底对账（reconciliation）
--   - 月度结算（settlement）
--   - 凭证（voucher + voucher_detail）按日报
-- ============================================================

USE banquet;
SET @start_date = '2026-04-01';
SET @end_date   = '2026-07-29';
SET @store1 = 1;
SET @store2 = 2;

-- 清空旧测试数据（保留结构）
TRUNCATE TABLE finance_voucher_detail;
TRUNCATE TABLE finance_voucher;
TRUNCATE TABLE finance_transaction;
TRUNCATE TABLE finance_settlement;
TRUNCATE TABLE finance_expense;
TRUNCATE TABLE finance_reconciliation;
TRUNCATE TABLE finance_payable;
TRUNCATE TABLE finance_receivable;
TRUNCATE TABLE finance_cost_record;
TRUNCATE TABLE finance_payment_record;
TRUNCATE TABLE finance_account;

-- ============================================================
-- 1. finance_account：4 个账户（2店×对公+现金）
-- ============================================================
INSERT INTO finance_account
  (store_id, account_code, account_name, account_type, bank_name, bank_account, account_holder, initial_balance, current_balance, is_active, sort_order, remark)
VALUES
  (1, 'NG-CORP-001', '宁国店对公账户', 'corp', '工商银行宁国支行', '1102021009900123456', '又见炊烟餐饮宁国店', 500000.00, 628600.00, 1, 1, '主结算户'),
  (1, 'NG-CASH-001', '宁国店现金账户', 'cash', NULL, NULL, '宁国店出纳', 50000.00, 32400.00, 1, 2, '备用金'),
  (2, 'XC-CORP-001', '宣城店对公账户', 'corp', '建设银行宣城支行', '1102021009900654321', '又见炊烟餐饮宣城店', 300000.00, 412800.00, 1, 1, '主结算户'),
  (2, 'XC-CASH-001', '宣城店现金账户', 'cash', NULL, NULL, '宣城店出纳', 30000.00, 18600.00, 1, 2, '备用金');

-- ============================================================
-- 2. finance_payment_record：每日客户付款（定金+尾款）
-- 规则：宁国店日均 5 单、宣城店日均 3 单，金额 800-8000
-- ============================================================
DROP PROCEDURE IF EXISTS seed_payment_record;
DELIMITER //
CREATE PROCEDURE seed_payment_record()
BEGIN
  DECLARE d DATE;
  DECLARE sid INT;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store1
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date
    UNION ALL
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store2
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO d, sid;
    IF done = 1 THEN LEAVE read_loop; END IF;
    -- 宁国店 5 单/日，宣城店 3 单/日
    SET @cnt = IF(sid = 1, 5, 3);
    SET @i = 0;
    WHILE @i < @cnt DO
      SET @amt = ROUND(800 + RAND() * 7200, 2);
      SET @method = ELT(FLOOR(RAND() * 5) + 1, 'wechat', 'alipay', 'cash', 'wechat', 'bank');
      SET @pno = CONCAT('PAY', DATE_FORMAT(d, '%Y%m%d'), LPAD(sid,2,'0'), LPAD(@i+1,3,'0'));
      INSERT INTO finance_payment_record
        (store_id, payment_no, payment_date, customer_name, amount, payment_method, account_id, operator_name, remark)
      VALUES
        (sid, @pno, d,
         ELT(FLOOR(RAND()*10)+1, '张先生','李女士','王先生','赵女士','陈先生','刘女士','杨先生','黄女士','周先生','吴女士'),
         @amt, @method, IF(sid=1, IF(@method='cash',2,1), IF(@method='cash',4,3)),
         IF(sid=1,'张婧','宣城店长'),
         IF(RAND()>0.5,'宴会定金','宴会尾款'));
      SET @i = @i + 1;
    END WHILE;
  END LOOP;
  CLOSE cur;
END//
DELIMITER ;
CALL seed_payment_record();
DROP PROCEDURE seed_payment_record;

-- ============================================================
-- 3. finance_cost_record：每日成本（食材40%/人工30%/能耗15%/其他15%）
-- ============================================================
DROP PROCEDURE IF EXISTS seed_cost_record;
DELIMITER //
CREATE PROCEDURE seed_cost_record()
BEGIN
  DECLARE d DATE;
  DECLARE sid INT;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store1
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date
    UNION ALL
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store2
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO d, sid;
    IF done = 1 THEN LEAVE read_loop; END IF;
    -- 4 类成本：food / labor / utility / other
    INSERT INTO finance_cost_record (store_id, cost_date, cost_type, cost_category, amount, operator_name, remark) VALUES
      (sid, d, 'food', '食材采购', ROUND(1500 + RAND() * 2500, 2), IF(sid=1,'张婧','宣城店长'), '当日食材消耗'),
      (sid, d, 'labor', '员工工资', ROUND(1200 + RAND() * 800, 2), IF(sid=1,'张婧','宣城店长'), '日工资计提'),
      (sid, d, 'utility', '水电燃气', ROUND(400 + RAND() * 600, 2), IF(sid=1,'张婧','宣城店长'), '日能耗'),
      (sid, d, 'other', '杂项支出', ROUND(150 + RAND() * 350, 2), IF(sid=1,'张婧','宣城店长'), '其他日常支出');
  END LOOP;
  CLOSE cur;
END//
DELIMITER ;
CALL seed_cost_record();
DROP PROCEDURE seed_cost_record;

-- ============================================================
-- 4. finance_receivable：应收账款（宴会尾款待收）
-- ============================================================
INSERT INTO finance_receivable
  (store_id, receivable_no, customer_name, total_amount, received_amount, pending_amount, receivable_date, due_date, status, credit_days, operator_name, remark)
VALUES
  (1, 'RV-2026-0501-01', '王先生', 8800.00, 2640.00, 6160.00, '2026-05-01', '2026-05-31', 'partial', 30, '张婧', '婚宴尾款'),
  (1, 'RV-2026-0515-02', '李女士', 12800.00, 3840.00, 8960.00, '2026-05-15', '2026-06-15', 'partial', 30, '张婧', '寿宴尾款'),
  (1, 'RV-2026-0601-01', '赵女士', 16800.00, 5040.00, 11760.00, '2026-06-01', '2026-07-01', 'partial', 30, '张婧', '升学宴尾款'),
  (1, 'RV-2026-0620-01', '陈先生', 9800.00, 0.00, 9800.00, '2026-06-20', '2026-07-20', 'unpaid', 30, '张婧', '商务宴尾款'),
  (1, 'RV-2026-0701-01', '刘女士', 22800.00, 6840.00, 15960.00, '2026-07-01', '2026-07-31', 'partial', 30, '张婧', '婚宴尾款'),
  (1, 'RV-2026-0710-01', '杨先生', 5800.00, 0.00, 5800.00, '2026-07-10', '2026-08-10', 'unpaid', 30, '张婧', '生日宴尾款'),
  (1, 'RV-2026-0715-01', '黄女士', 14800.00, 4440.00, 10360.00, '2026-07-15', '2026-08-15', 'partial', 30, '张婧', '满月宴尾款'),
  (1, 'RV-2026-0720-01', '周先生', 18800.00, 5640.00, 13160.00, '2026-07-20', '2026-08-20', 'partial', 30, '张婧', '婚宴尾款'),
  (1, 'RV-2026-0725-01', '吴女士', 7800.00, 0.00, 7800.00, '2026-07-25', '2026-08-25', 'unpaid', 30, '张婧', '乔迁宴尾款'),
  (1, 'RV-2026-0728-01', '徐先生', 32800.00, 9840.00, 22960.00, '2026-07-28', '2026-08-28', 'partial', 30, '张婧', '婚宴尾款'),
  (2, 'RV-2026-0605-01', '孙先生', 6800.00, 2040.00, 4760.00, '2026-06-05', '2026-07-05', 'partial', 30, '宣城店长', '婚宴尾款'),
  (2, 'RV-2026-0618-01', '马女士', 9800.00, 2940.00, 6860.00, '2026-06-18', '2026-07-18', 'partial', 30, '宣城店长', '寿宴尾款'),
  (2, 'RV-2026-0705-01', '朱先生', 12800.00, 0.00, 12800.00, '2026-07-05', '2026-08-05', 'unpaid', 30, '宣城店长', '升学宴尾款'),
  (2, 'RV-2026-0712-01', '胡女士', 5800.00, 1740.00, 4060.00, '2026-07-12', '2026-08-12', 'partial', 30, '宣城店长', '生日宴尾款'),
  (2, 'RV-2026-0720-01', '林先生', 16800.00, 5040.00, 11760.00, '2026-07-20', '2026-08-20', 'partial', 30, '宣城店长', '商务宴尾款');

-- ============================================================
-- 5. finance_payable：应付账款（供应商货款待付）
-- ============================================================
INSERT INTO finance_payable
  (store_id, payable_no, supplier_name, total_amount, paid_amount, pending_amount, payable_date, due_date, status, credit_days, operator_name, remark)
VALUES
  (1, 'PY-2026-0610-01', '鑫源食品有限公司', 18500.00, 0.00, 18500.00, '2026-06-10', '2026-07-10', 'unpaid', 30, '张婧', '海鲜月结'),
  (1, 'PY-2026-0615-01', '绿源蔬菜基地', 6800.00, 3400.00, 3400.00, '2026-06-15', '2026-07-15', 'partial', 30, '张婧', '蔬菜半月结'),
  (1, 'PY-2026-0620-01', '宁国酒水批发', 12800.00, 0.00, 12800.00, '2026-06-20', '2026-07-20', 'unpaid', 30, '张婧', '酒水月结'),
  (1, 'PY-2026-0625-01', '安庆肉类联合社', 22500.00, 11000.00, 11500.00, '2026-06-25', '2026-07-25', 'partial', 30, '张婧', '猪肉月结'),
  (1, 'PY-2026-0701-01', '宣纸文化用品', 1800.00, 0.00, 1800.00, '2026-07-01', '2026-08-01', 'unpaid', 30, '张婧', '宴会用品'),
  (1, 'PY-2026-0710-01', '宁国燃气公司', 4200.00, 2100.00, 2100.00, '2026-07-10', '2026-08-10', 'partial', 30, '张婧', '燃气月结'),
  (1, 'PY-2026-0715-01', '国网安徽电力', 8800.00, 0.00, 8800.00, '2026-07-15', '2026-08-15', 'unpaid', 30, '张婧', '电费月结'),
  (1, 'PY-2026-0720-01', '宁国市水务公司', 2200.00, 0.00, 2200.00, '2026-07-20', '2026-08-20', 'unpaid', 30, '张婧', '水费月结'),
  (2, 'PY-2026-0612-01', '宣城食品供应商', 12800.00, 0.00, 12800.00, '2026-06-12', '2026-07-12', 'unpaid', 30, '宣城店长', '食材月结'),
  (2, 'PY-2026-0618-01', '宣城酒水批发', 8800.00, 4400.00, 4400.00, '2026-06-18', '2026-07-18', 'partial', 30, '宣城店长', '酒水半月结'),
  (2, 'PY-2026-0705-01', '宣城肉类批发', 15600.00, 0.00, 15600.00, '2026-07-05', '2026-08-05', 'unpaid', 30, '宣城店长', '肉类月结'),
  (2, 'PY-2026-0710-01', '宣城燃气公司', 2800.00, 1400.00, 1400.00, '2026-07-10', '2026-08-10', 'partial', 30, '宣城店长', '燃气月结'),
  (2, 'PY-2026-0718-01', '宣城电力公司', 5600.00, 0.00, 5600.00, '2026-07-18', '2026-08-18', 'unpaid', 30, '宣城店长', '电费月结');

-- ============================================================
-- 6. finance_reconciliation：宴会定金对账
-- ============================================================
INSERT INTO finance_reconciliation
  (store_id, recon_no, recon_date, account_name, book_balance, bank_balance, diff_amount, status, operator_name, remark)
VALUES
  (1, 'RC-2026-0501', '2026-05-01', '宁国店对公账户', 528600.00, 528450.00, -150.00, 'pending', '张婧', '5月对账-手续费差异'),
  (1, 'RC-2026-0601', '2026-06-01', '宁国店对公账户', 612800.00, 612800.00, 0.00, 'pending', '张婧', '6月对账-无误'),
  (1, 'RC-2026-0701', '2026-07-01', '宁国店对公账户', 624300.00, 624180.00, -120.00, 'pending', '张婧', '7月对账-在途资金'),
  (2, 'RC-2026-0501', '2026-05-01', '宣城店对公账户', 318500.00, 318500.00, 0.00, 'pending', '宣城店长', '5月对账-无误'),
  (2, 'RC-2026-0601', '2026-06-01', '宣城店对公账户', 405200.00, 405100.00, -100.00, 'pending', '宣城店长', '6月对账-手续费'),
  (2, 'RC-2026-0701', '2026-07-01', '宣城店对公账户', 412800.00, 412800.00, 0.00, 'pending', '宣城店长', '7月对账-无误');

-- ============================================================
-- 7. finance_settlement：月度结算（2店×3月=6条）
-- ============================================================
INSERT INTO finance_settlement
  (store_id, settlement_no, settlement_date, settlement_type, start_date, end_date, total_income, total_expense, total_profit, food_cost, labor_cost, rent_cost, utility_cost, other_cost, cost_rate, status, operator_name, remark)
VALUES
  (1, 'ST-202605-01', '2026-05-31', 'monthly', '2026-05-01', '2026-05-31', 285000.00, 198000.00, 87000.00, 105000.00, 56000.00, 18000.00, 12000.00, 7000.00, 69.5, 'posted', '张婧', '5月月结-毛利率30.5%'),
  (1, 'ST-202606-01', '2026-06-30', 'monthly', '2026-06-01', '2026-06-30', 318000.00, 220000.00, 98000.00, 118000.00, 58000.00, 18000.00, 13500.00, 12500.00, 69.2, 'posted', '张婧', '6月月结-毛利率30.8%'),
  (1, 'ST-202607-01', '2026-07-31', 'monthly', '2026-07-01', '2026-07-31', 156000.00, 108000.00, 48000.00, 58000.00, 30000.00, 9000.00, 7000.00, 4000.00, 69.2, 'draft', '张婧', '7月在结'),
  (2, 'ST-202605-02', '2026-05-31', 'monthly', '2026-05-01', '2026-05-31', 168000.00, 118000.00, 50000.00, 62000.00, 32000.00, 12000.00, 8000.00, 4000.00, 70.2, 'posted', '宣城店长', '5月月结-毛利率29.8%'),
  (2, 'ST-202606-02', '2026-06-30', 'monthly', '2026-06-01', '2026-06-30', 192000.00, 134000.00, 58000.00, 70000.00, 34000.00, 12000.00, 9000.00, 9000.00, 69.8, 'posted', '宣城店长', '6月月结-毛利率30.2%'),
  (2, 'ST-202607-02', '2026-07-31', 'monthly', '2026-07-01', '2026-07-31', 88000.00, 62000.00, 26000.00, 33000.00, 16000.00, 6000.00, 4000.00, 3000.00, 70.5, 'draft', '宣城店长', '7月在结');

-- ============================================================
-- 8. finance_expense：费用报销
-- ============================================================
INSERT INTO finance_expense
  (store_id, expense_no, expense_type, expense_date, applicant_name, department, amount, invoice_amount, approval_status, approver_name, payment_status, remark)
VALUES
  (1, 'EX-2026-0410-01', '办公用品', '2026-04-10', '张婧', '总经办', 580.00, 580.00, 'approved', 'rino', 'paid', '打印纸+硒鼓'),
  (1, 'EX-2026-0418-01', '差旅费', '2026-04-18', '王经理', '前厅部', 1280.00, 1280.00, 'approved', 'rino', 'paid', '合肥考察'),
  (1, 'EX-2026-0508-01', '维修费', '2026-05-08', '李主管', '工程部', 2800.00, 2800.00, 'approved', 'rino', 'paid', '空调维修'),
  (1, 'EX-2026-0515-01', '招待费', '2026-05-15', '张婧', '总经办', 1680.00, 1680.00, 'approved', 'rino', 'paid', '客户招待'),
  (1, 'EX-2026-0602-01', '培训费', '2026-06-02', '陈主管', '前厅部', 3200.00, 3200.00, 'approved', 'rino', 'paid', '服务礼仪培训'),
  (1, 'EX-2026-0618-01', '办公用品', '2026-06-18', '张婧', '总经办', 880.00, 880.00, 'pending', NULL, 'unpaid', '宣传册印刷'),
  (1, 'EX-2026-0705-01', '维修费', '2026-07-05', '李主管', '工程部', 4500.00, 4500.00, 'pending', NULL, 'unpaid', '厨房排烟系统'),
  (1, 'EX-2026-0712-01', '差旅费', '2026-07-12', '赵经理', '采购部', 1880.00, 1880.00, 'pending', NULL, 'unpaid', '供应商考察'),
  (1, 'EX-2026-0720-01', '招待费', '2026-07-20', '张婧', '总经办', 2280.00, 2280.00, 'pending', NULL, 'unpaid', '重要客户宴请'),
  (2, 'EX-2026-0415-02', '办公用品', '2026-04-15', '宣城店长', '总经办', 380.00, 380.00, 'approved', 'rino', 'paid', '日常办公'),
  (2, 'EX-2026-0520-02', '维修费', '2026-05-20', '宣城店长', '工程部', 1800.00, 1800.00, 'approved', 'rino', 'paid', '电梯保养'),
  (2, 'EX-2026-0708-02', '培训费', '2026-07-08', '宣城店长', '前厅部', 2400.00, 2400.00, 'pending', NULL, 'unpaid', '服务培训');

-- ============================================================
-- 9. finance_transaction：收支流水（每日每店3-5条）
-- 简化为：每店每日 2 条收 + 2 条支
-- ============================================================
DROP PROCEDURE IF EXISTS seed_transaction;
DELIMITER //
CREATE PROCEDURE seed_transaction()
BEGIN
  DECLARE d DATE;
  DECLARE sid INT;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store1
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date
    UNION ALL
    SELECT DATE_ADD(@start_date, INTERVAL n DAY) AS d, @store2
    FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
          UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
          UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18
          UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
          UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
          UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION SELECT 36
          UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41 UNION SELECT 42
          UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48
          UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
          UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
          UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION SELECT 66
          UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION SELECT 71 UNION SELECT 72
          UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION SELECT 76 UNION SELECT 77 UNION SELECT 78
          UNION SELECT 79 UNION SELECT 80 UNION SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84
          UNION SELECT 85 UNION SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90
          UNION SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION SELECT 96
          UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100 UNION SELECT 101 UNION SELECT 102
          UNION SELECT 103 UNION SELECT 104 UNION SELECT 105 UNION SELECT 106 UNION SELECT 107 UNION SELECT 108
          UNION SELECT 109 UNION SELECT 110 UNION SELECT 111 UNION SELECT 112 UNION SELECT 113 UNION SELECT 114
          UNION SELECT 115 UNION SELECT 116 UNION SELECT 117 UNION SELECT 118 UNION SELECT 119) days
    WHERE DATE_ADD(@start_date, INTERVAL n DAY) <= @end_date;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO d, sid;
    IF done = 1 THEN LEAVE read_loop; END IF;
    INSERT INTO finance_transaction
      (store_id, trans_no, trans_date, trans_time, trans_type, trans_category, account_id, amount, payer_payee, payment_method, operator_name, remark)
    VALUES
      (sid, CONCAT('TX', DATE_FORMAT(d,'%Y%m%d'), LPAD(sid,2,'0'), '01', LPAD(FLOOR(RAND()*1000),3,'0')),
       d, TIMESTAMP(d, '10:30:00'), 'income', '宴会收款', IF(sid=1,1,3), ROUND(2000+RAND()*6000,2),
       '宴会客户', 'wechat', IF(sid=1,'张婧','宣城店长'), '上午收款'),
      (sid, CONCAT('TX', DATE_FORMAT(d,'%Y%m%d'), LPAD(sid,2,'0'), '02', LPAD(FLOOR(RAND()*1000),3,'0')),
       d, TIMESTAMP(d, '12:00:00'), 'income', '散客收款', IF(sid=1,2,4), ROUND(800+RAND()*3000,2),
       '散客', 'cash', IF(sid=1,'张婧','宣城店长'), '午市'),
      (sid, CONCAT('TX', DATE_FORMAT(d,'%Y%m%d'), LPAD(sid,2,'0'), '03', LPAD(FLOOR(RAND()*1000),3,'0')),
       d, TIMESTAMP(d, '15:00:00'), 'expense', '食材采购', IF(sid=1,1,3), ROUND(1500+RAND()*2500,2),
       '鑫源食品', 'bank', IF(sid=1,'张婧','宣城店长'), '当日采购'),
      (sid, CONCAT('TX', DATE_FORMAT(d,'%Y%m%d'), LPAD(sid,2,'0'), '04', LPAD(FLOOR(RAND()*1000),3,'0')),
       d, TIMESTAMP(d, '18:00:00'), 'expense', '员工工资', IF(sid=1,1,3), ROUND(1200+RAND()*800,2),
       '员工', 'cash', IF(sid=1,'张婧','宣城店长'), '日工资');
  END LOOP;
  CLOSE cur;
END//
DELIMITER ;
CALL seed_transaction();
DROP PROCEDURE seed_transaction;

-- ============================================================
-- 10+11. finance_voucher + finance_voucher_detail：会计凭证
-- 每店每月 5 张凭证 + 每张 3 行分录
-- ============================================================
DROP PROCEDURE IF EXISTS seed_voucher;
DELIMITER //
CREATE PROCEDURE seed_voucher()
BEGIN
  DECLARE d DATE;
  DECLARE sid INT;
  DECLARE m INT;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR
    SELECT '2026-05-31' AS d, 1 AS sid, 1 AS m UNION ALL
    SELECT '2026-06-30', 1, 2 UNION ALL SELECT '2026-07-30', 1, 3 UNION ALL
    SELECT '2026-05-31', 2, 1 UNION ALL SELECT '2026-06-30', 2, 2 UNION ALL
    SELECT '2026-07-30', 2, 3;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO d, sid, m;
    IF done = 1 THEN LEAVE read_loop; END IF;
    -- 月度凭证 1：收入结转
    INSERT INTO finance_voucher
      (store_id, voucher_no, voucher_date, voucher_type, summary, total_debit, total_credit, is_balanced, status, prepared_name, audited_name, posted_by)
    VALUES
      (sid, CONCAT('V-', DATE_FORMAT(d,'%Y%m'), '-', LPAD(sid,2,'0'), '01'), d, 'transfer',
       CONCAT(IF(sid=1,'宁国店','宣城店'), m, '月收入结转'),
       150000.00, 150000.00, 1, 'posted', IF(sid=1,'张婧','宣城店长'), 'rino', 1);
    SET @vid = LAST_INSERT_ID();
    INSERT INTO finance_voucher_detail
      (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount)
    VALUES
      (@vid, sid, 1, '1001', '库存现金', '结转现金收入', 50000.00, 0.00),
      (@vid, sid, 2, '1002', '银行存款', '结转对公收款', 100000.00, 0.00),
      (@vid, sid, 3, '6001', '主营业务收入', '结转收入', 0.00, 150000.00);
    -- 月度凭证 2：成本结转
    INSERT INTO finance_voucher
      (store_id, voucher_no, voucher_date, voucher_type, summary, total_debit, total_credit, is_balanced, status, prepared_name, audited_name, posted_by)
    VALUES
      (sid, CONCAT('V-', DATE_FORMAT(d,'%Y%m'), '-', LPAD(sid,2,'0'), '02'), d, 'transfer',
       CONCAT(IF(sid=1,'宁国店','宣城店'), m, '月成本结转'),
       95000.00, 95000.00, 1, 'posted', IF(sid=1,'张婧','宣城店长'), 'rino', 1);
    SET @vid = LAST_INSERT_ID();
    INSERT INTO finance_voucher_detail
      (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount)
    VALUES
      (@vid, sid, 1, '6401', '主营业务成本', '食材成本', 55000.00, 0.00),
      (@vid, sid, 2, '6402', '人工成本', '员工工资', 28000.00, 0.00),
      (@vid, sid, 3, '6403', '能耗成本', '水电燃气', 12000.00, 0.00),
      (@vid, sid, 4, '1405', '库存商品', '结转成本', 0.00, 95000.00);
    -- 月度凭证 3：费用结转
    INSERT INTO finance_voucher
      (store_id, voucher_no, voucher_date, voucher_type, summary, total_debit, total_credit, is_balanced, status, prepared_name, audited_name, posted_by)
    VALUES
      (sid, CONCAT('V-', DATE_FORMAT(d,'%Y%m'), '-', LPAD(sid,2,'0'), '03'), d, 'transfer',
       CONCAT(IF(sid=1,'宁国店','宣城店'), m, '月期间费用'),
       25000.00, 25000.00, 1, 'posted', IF(sid=1,'张婧','宣城店长'), 'rino', 1);
    SET @vid = LAST_INSERT_ID();
    INSERT INTO finance_voucher_detail
      (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount)
    VALUES
      (@vid, sid, 1, '6601', '管理费用', '办公+差旅+招待', 15000.00, 0.00),
      (@vid, sid, 2, '6602', '销售费用', '宣传+广告', 5000.00, 0.00),
      (@vid, sid, 3, '6603', '财务费用', '手续费+利息', 5000.00, 0.00),
      (@vid, sid, 4, '1002', '银行存款', '支付费用', 0.00, 25000.00);
  END LOOP;
  CLOSE cur;
END//
DELIMITER ;
CALL seed_voucher();
DROP PROCEDURE seed_voucher;

-- ============================================================
-- 校验
-- ============================================================
SELECT 'finance_account' AS tbl, COUNT(*) AS cnt FROM finance_account
UNION ALL SELECT 'finance_payment_record', COUNT(*) FROM finance_payment_record
UNION ALL SELECT 'finance_cost_record', COUNT(*) FROM finance_cost_record
UNION ALL SELECT 'finance_receivable', COUNT(*) FROM finance_receivable
UNION ALL SELECT 'finance_payable', COUNT(*) FROM finance_payable
UNION ALL SELECT 'finance_reconciliation', COUNT(*) FROM finance_reconciliation
UNION ALL SELECT 'finance_settlement', COUNT(*) FROM finance_settlement
UNION ALL SELECT 'finance_expense', COUNT(*) FROM finance_expense
UNION ALL SELECT 'finance_transaction', COUNT(*) FROM finance_transaction
UNION ALL SELECT 'finance_voucher', COUNT(*) FROM finance_voucher
UNION ALL SELECT 'finance_voucher_detail', COUNT(*) FROM finance_voucher_detail;
