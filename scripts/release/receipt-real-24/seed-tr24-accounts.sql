-- TR-RECEIPT-REAL-24 合成账号（仅隔离 schema co_print23_20260909_022305，全是合成数据）
-- 与真实生产账号无任何对应；TR24 标识账号仅用于小票接口身份/门店范围断言。
-- 密码哈希复用本 schema coprint23_manager 的 BCrypt 哈希，密码同为 123456（隔离实例专用）。
-- r2 返修：去掉硬 DELETE 与默认重种路径。本脚本只 INSERT：
--   主键/唯一键冲突会直接报错拒绝（不覆盖、不更新旧行）；默认复放不执行本脚本，
--   仅在显式初始化（运行器 -InitSeed）时执行一次。
SET NAMES utf8mb4;

INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,staff_password,role,employment_status) VALUES
 (923101,1,'TR24合成总经理','tr24_gm',     '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6','gm',    'active'),
 (923103,1,'TR24一店员工',  'tr24_staff1', '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6','staff', 'active'),
 (923102,2,'TR24二店员工',  'tr24_staff2', '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6','staff', 'active');

SELECT 'TR24_SEED_OK' AS result, COUNT(*) AS tr24_staff FROM staff_master WHERE staff_account LIKE 'tr24_%';
