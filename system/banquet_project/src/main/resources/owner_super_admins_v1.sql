-- 又见炊烟私房菜：老板超级管理员账号（幂等）
-- 密码仅保存 BCrypt 摘要，不在数据库或代码中保存明文。

UPDATE staff_master
SET staff_name = 'rino',
    staff_password = '$2b$12$MmFeO2mq7JDR0AOvKvbpL.LkJVH5iLJuKKFMnPPPXp4DSAtPW1DAS',
    staff_position = '老板',
    department = '总经办',
    employment_status = 'active',
    role = 'super_admin',
    permission_level = 99,
    can_manage_kitchen = 1,
    can_manage_sales = 1,
    can_manage_finance = 1,
    can_manage_hr = 1,
    can_view_all_stores = 1,
    can_edit_system = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE staff_account = 'rino';

INSERT INTO staff_master (
    store_id, staff_no, staff_name, staff_account, staff_password,
    staff_position, department, employment_status, role, permission_level,
    can_manage_kitchen, can_manage_sales, can_manage_finance, can_manage_hr,
    can_view_all_stores, can_edit_system, created_at, updated_at
)
SELECT
    1, 'OWNER-RINO', 'rino', 'rino',
    '$2b$12$MmFeO2mq7JDR0AOvKvbpL.LkJVH5iLJuKKFMnPPPXp4DSAtPW1DAS',
    '老板', '总经办', 'active', 'super_admin', 99,
    1, 1, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM staff_master WHERE staff_account = 'rino'
);

UPDATE staff_master
SET staff_name = '张婧',
    staff_password = '$2b$12$zhbvge0pSVaLKEvEqapHW.UVJb.ve8Iy3Sxw3mNVK89AjxADrF0M.',
    staff_position = '老板',
    department = '总经办',
    employment_status = 'active',
    role = 'super_admin',
    permission_level = 99,
    can_manage_kitchen = 1,
    can_manage_sales = 1,
    can_manage_finance = 1,
    can_manage_hr = 1,
    can_view_all_stores = 1,
    can_edit_system = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE staff_account = '张婧';

INSERT INTO staff_master (
    store_id, staff_no, staff_name, staff_account, staff_password,
    staff_position, department, employment_status, role, permission_level,
    can_manage_kitchen, can_manage_sales, can_manage_finance, can_manage_hr,
    can_view_all_stores, can_edit_system, created_at, updated_at
)
SELECT
    1, 'OWNER-ZHANGJING', '张婧', '张婧',
    '$2b$12$zhbvge0pSVaLKEvEqapHW.UVJb.ve8Iy3Sxw3mNVK89AjxADrF0M.',
    '老板', '总经办', 'active', 'super_admin', 99,
    1, 1, 1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM staff_master WHERE staff_account = '张婧'
);

SELECT staff_id, staff_name, staff_account, staff_position, role,
       permission_level, can_view_all_stores, can_edit_system
FROM staff_master
WHERE staff_account IN ('rino', '张婧')
ORDER BY staff_account;
