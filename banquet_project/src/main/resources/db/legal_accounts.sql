-- =====================================================================
-- 法务案卷访问账号（在生产库执行）
-- 口令说明：本文件只保存 BCrypt 哈希（$2a$, cost=10，与 Spring BCryptPasswordEncoder 兼容），
--   不记录任何明文口令，也不写明口令的生成规则。
--   临时口令由管理员当面或通过其他渠道单独告知本人，首次登录后必须各自改掉。
--   注意：本文件的历史版本曾写有明文口令（且口令规则为手机号后六位），
--   凡在此之前下发过的口令一律视为已泄露。
--   三个账号（zhangju / zhangjing / rino）的哈希均已重新生成，旧口令一律作废；
--   新口令由管理员单独告知本人，本文件不记录明文。
--   每个账号单独加盐：即便两人口令相同，文件中的哈希串也不同，
--   看文件的人无法从哈希判断哪两个账号口令一致。
--   日后再改口令：任一 Spring 环境执行
--     new BCryptPasswordEncoder().encode("新口令")
--   把输出的 $2a$ 串贴到对应位置，切勿在本文件写入明文。
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

-- 若上面因已存在而没插入，则更新口令、角色，并补齐姓名与拼音账号，
-- 保证「张炬」「zhangju」「13485942305」三种写法都能登录：
UPDATE staff_master
   SET staff_password = '$2a$10$gW9inuvb94hbYgKB1RU2ie3U1OX7auYMaRAK2zt7sgLJM6PlP6hcC',
       staff_name    = '张炬',
       staff_account = 'zhangju',
       role = 'lawyer',
       employment_status = 'active'
 WHERE staff_phone = '13485942305';

-- ---------------------------------------------------------------
-- 张婧   账号 zhangjing   手机 13805638866
-- ---------------------------------------------------------------
-- 三种写法都要能登录：中文姓名「张婧」/ 拼音账号「zhangjing」/ 手机号「13805638866」
-- 因此除口令外，必须一并补齐 staff_name 与 staff_account，缺一种就少一条登录途径。
UPDATE staff_master
   SET staff_password = '$2a$10$5lf070z6FxfZ6Vx7ts8KC.JddVH2JjU4mWlUjnIkAtYj9KfW7mH8m',
       staff_name    = '张婧',
       staff_account = 'zhangjing',
       employment_status = 'active'
 WHERE staff_phone = '13805638866';
-- 若上面影响 0 行，说明花名册里还没这个人，用下面这条建：
-- INSERT INTO staff_master (store_id, staff_name, staff_account, staff_password,
--        staff_phone, staff_position, department, employment_status, role, created_at)
-- VALUES (@store_id, '张婧', 'zhangjing', '$2a$10$5lf070z6FxfZ6Vx7ts8KC.JddVH2JjU4mWlUjnIkAtYj9KfW7mH8m', '13805638866', '负责人', '管理', 'active', 'admin', NOW());

-- ---------------------------------------------------------------
-- Rino   账号 rino   手机 18605638866
-- ---------------------------------------------------------------
-- 三种写法都要能登录：姓名 / 账号「rino」/ 手机号「18605638866」。
-- staff_en_name 若这套库里没有该列，删掉那一行再执行即可。
UPDATE staff_master
   SET staff_password = '$2a$10$VRixT3IMg/TeptY6ZJ.jO.ZC/hOI0NLVbPJj55kjsxcOgGZ7LwEkG',
       staff_account = 'rino',
       staff_en_name = 'Rino',
       employment_status = 'active'
 WHERE staff_phone = '18605638866';
-- 若上面影响 0 行，说明花名册里还没这个人，用下面这条建：
-- INSERT INTO staff_master (store_id, staff_name, staff_account, staff_password,
--        staff_phone, staff_position, department, employment_status, role, created_at)
-- VALUES (@store_id, 'Rino', 'rino', '$2a$10$VRixT3IMg/TeptY6ZJ.jO.ZC/hOI0NLVbPJj55kjsxcOgGZ7LwEkG', '18605638866', '负责人', '管理', 'active', 'admin', NOW());

-- 【第三步】核对结果
-- (1) role 必须落在 legal.allowed-roles 里（默认 lawyer,gm,super_admin,admin），
--     口令必须是 BCrypt 哈希，姓名/账号/手机号三项都不能为空：
SELECT staff_id, staff_name, staff_account, staff_en_name, staff_phone, role, employment_status,
       CASE WHEN staff_password LIKE '$2%' THEN 'BCRYPT' ELSE '明文!!' END AS pwd,
       CASE WHEN staff_name    IS NULL OR staff_name    = '' THEN '缺中文名'  ELSE 'OK' END AS chk_name,
       CASE WHEN staff_account IS NULL OR staff_account = '' THEN '缺拼音账号' ELSE 'OK' END AS chk_account,
       CASE WHEN staff_phone   IS NULL OR staff_phone   = '' THEN '缺手机号'   ELSE 'OK' END AS chk_phone
  FROM staff_master
 WHERE staff_phone IN ('13485942305','13805638866','18605638866');

-- (2) 登录按「手机号 → 账号 → 英文名 → 姓名」逐项匹配，每一项内部必须唯一，
--     命中多条那一项就用不了。下面这条列出会造成冲突的记录，结果应为空：
SELECT '手机号' AS 冲突项, staff_phone AS 值, COUNT(*) AS 条数 FROM staff_master
 WHERE employment_status IN ('active','在职') AND staff_phone IN ('13485942305','13805638866','18605638866')
 GROUP BY staff_phone HAVING COUNT(*) > 1
UNION ALL
SELECT '账号', staff_account, COUNT(*) FROM staff_master
 WHERE employment_status IN ('active','在职') AND staff_account IN ('zhangju','zhangjing','rino')
 GROUP BY staff_account HAVING COUNT(*) > 1
UNION ALL
SELECT '姓名', staff_name, COUNT(*) FROM staff_master
 WHERE employment_status IN ('active','在职') AND staff_name IN ('张炬','张婧')
 GROUP BY staff_name HAVING COUNT(*) > 1;
-- 若(2)有结果，说明花名册里有重名/重号，登录会返回 409 提示改用手机号；
-- 请先清理重复记录（历史上 rino 就同时存在拼音号 id200 与英文号 id204 两条）。
