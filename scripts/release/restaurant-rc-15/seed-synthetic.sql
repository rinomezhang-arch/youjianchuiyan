-- TR-RELEASE-RC-15 合成种子（仅隔离库 banquet_rc15，全是合成数据）
-- 与真实生产账号无任何对应；staff 401-404 为纯合成测试账号，明文密码仅在此隔离实例使用
-- 复跑安全：先 DELETE 再 INSERT，可重复执行
SET NAMES utf8mb4;

DELETE FROM finance_account WHERE account_id IN (9001,9002) OR account_name LIKE '%真实端口%' OR account_name LIKE '%合成%' OR account_name LIKE '%降权后%' OR account_name LIKE '%停用后%' OR account_name LIKE '%在职员工%';
DELETE FROM audit_logs WHERE user_id IN ('401','402','403','404');
DELETE FROM staff_master WHERE staff_id BETWEEN 401 AND 404;
DELETE FROM store_info WHERE store_id IN (1,2) AND store_code IN ('RC15S1','RC15S2');

INSERT INTO store_info(store_id,store_code,store_name) VALUES
 (1,'RC15S1','合成一店'),
 (2,'RC15S2','合成二店');

-- 登录方式：POST /api/auth/login {username: staff_account, password: 'synpass123'}
-- 账号/姓名/手机号/英文名四选一都可登录；这里用 staff_account
INSERT INTO staff_master(staff_id,staff_name,staff_account,role,store_id,employment_status,staff_password) VALUES
 (401,'合成调店员工','syn_mover',   'manager',1,'active','synpass123'),
 (402,'合成降权员工','syn_demoted', 'manager',1,'active','synpass123'),
 (403,'合成离职员工','syn_leaver',  'manager',1,'active','synpass123'),
 (404,'合成在职员工','syn_stayer',  'manager',1,'active','synpass123');

INSERT INTO finance_account(account_id,store_id,account_code,account_name,account_type,initial_balance,current_balance,is_active,sort_order,created_at) VALUES
 (9001,1,'ACC9001','一店现金','cash',0,0,1,0,NOW()),
 (9002,2,'ACC9002','二店现金','cash',0,0,1,0,NOW());

SELECT 'SEED_OK' AS result, COUNT(*) AS staff FROM staff_master WHERE staff_id BETWEEN 401 AND 404;
