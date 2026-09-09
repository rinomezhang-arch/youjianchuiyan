-- =====================================================================
-- 法务案卷访问账号（在生产库执行）
-- 口令说明：本文件只保存 BCrypt 哈希（$2a$, cost=10，与 Spring BCryptPasswordEncoder 兼容），
--   不记录任何明文口令，也不写明口令的生成规则。
--   临时口令由管理员当面或通过其他渠道单独告知本人，首次登录后必须各自改掉。
--   注意：本文件的历史版本曾写有明文口令（且口令规则为手机号后六位），
--   凡在此之前下发过的口令一律视为已泄露。
--   · 张炬（zhangju）：哈希已于本次更换，新口令由管理员单独告知本人。
--   · 张婧（zhangjing）、Rino：仍是旧哈希，执行前请先各自重新生成并替换。
--   重新生成哈希的办法：任一 Spring 环境执行
--     new BCryptPasswordEncoder().encode("新口令")
--   把输出的 $2a$ 串贴到下面对应位置，切勿在本文件写入明文。
-- =====================================================================

-- 【第一步】先看现状，确认张婧和 Rino 已有的账号，避免建重复记录：
SELECT staff_id, store_id, staff_name, staff_account, staff_phone, role, employment_status
  FROM staff_master
 WHERE staff_phone IN ('13485942305','13805638866','18605638866')
    OR staff_account IN ('zhangju','zhangjing','rino');

-- 【第二步】确认宁国店的 store_id：
SELECT store_id, store_name FROM store_master;
SET @store_id = 1;   -- ← 改成宁国店的真实 store_id

-- ---------------------------------------------------------------
-- 张炬   账号 zhangju   手机 13485942305
-- ---------------------------------------------------------------
INSERT INTO staff_master
    (store_id, staff_name, staff_account, staff_password, staff_phone,
     staff_position, department, employment_status, role, remark, created_at)
SELECT @store_id, '张炬', 'zhangju', '$2a$10$gW9inuvb94hbYgKB1RU2ie3U1OX7auYMaRAK2zt7sgLJM6PlP6hcC', '13485942305',
       '代理律师', '外部顾问', 'active', 'lawyer',
       '西津律师事务所。微信号 zhangju13485942305。仅授权查阅“宁国店消防改造合同纠纷”案卷，不参与经营业务。', NOW()
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM (SELECT staff_phone FROM staff_master) t
                    WHERE t.staff_phone = '13485942305');

-- 若上面因已存在而没插入，则只更新密码与角色：
UPDATE staff_master SET staff_password = '$2a$10$gW9inuvb94hbYgKB1RU2ie3U1OX7auYMaRAK2zt7sgLJM6PlP6hcC', role = 'lawyer',
       employment_status = 'active'
 WHERE staff_phone = '13485942305';

-- ---------------------------------------------------------------
-- 张婧   账号 zhangjing   手机 13805638866
-- ---------------------------------------------------------------
UPDATE staff_master SET staff_password = '$2a$10$x/skNFDUoVPxe8Du7aMkf.UkcwFugOn6QW7JcI05AKy.Xx/GP1NWq'
 WHERE staff_phone = '13805638866';
-- 若上面影响 0 行，说明花名册里还没这个人，用下面这条建：
-- INSERT INTO staff_master (store_id, staff_name, staff_account, staff_password,
--        staff_phone, staff_position, department, employment_status, role, created_at)
-- VALUES (@store_id, '张婧', 'zhangjing', '$2a$10$x/skNFDUoVPxe8Du7aMkf.UkcwFugOn6QW7JcI05AKy.Xx/GP1NWq', '13805638866', '负责人', '管理', 'active', 'admin', NOW());

-- ---------------------------------------------------------------
-- Rino   账号 rino   手机 18605638866
-- ---------------------------------------------------------------
UPDATE staff_master SET staff_password = '$2a$10$J.LhTQ63MET85/HX4xQhsO75ujYTfB/i2qrj4Wqd7BCcAe.6ooqe2'
 WHERE staff_phone = '18605638866';
-- 若上面影响 0 行，说明花名册里还没这个人，用下面这条建：
-- INSERT INTO staff_master (store_id, staff_name, staff_account, staff_password,
--        staff_phone, staff_position, department, employment_status, role, created_at)
-- VALUES (@store_id, 'Rino', 'rino', '$2a$10$J.LhTQ63MET85/HX4xQhsO75ujYTfB/i2qrj4Wqd7BCcAe.6ooqe2', '18605638866', '负责人', '管理', 'active', 'admin', NOW());

-- 【第三步】核对结果：role 必须落在 legal.allowed-roles 里
--          （默认 lawyer,gm,super_admin,admin）
SELECT staff_id, staff_name, staff_account, staff_phone, role, employment_status,
       CASE WHEN staff_password LIKE '$2%' THEN 'BCRYPT' ELSE '明文!!' END AS pwd
  FROM staff_master
 WHERE staff_phone IN ('13485942305','13805638866','18605638866');
