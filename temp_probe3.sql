SELECT '--- staff with salary data ---' AS info;
SELECT COUNT(*) AS total_staff, 
  SUM(CASE WHEN basic_salary IS NOT NULL THEN 1 ELSE 0 END) AS has_basic,
  SUM(CASE WHEN performance_salary IS NOT NULL THEN 1 ELSE 0 END) AS has_perf,
  SUM(CASE WHEN subsidy IS NOT NULL THEN 1 ELSE 0 END) AS has_subsidy,
  SUM(CASE WHEN bonus IS NOT NULL THEN 1 ELSE 0 END) AS has_bonus,
  SUM(CASE WHEN social_insurance IS NOT NULL THEN 1 ELSE 0 END) AS has_social,
  SUM(CASE WHEN housing_fund IS NOT NULL THEN 1 ELSE 0 END) AS has_housing
FROM staff_master WHERE employment_status='active' OR employment_status IS NULL;
SELECT '--- staff with salary sample ---' AS info;
SELECT staff_id, staff_name, department, basic_salary, performance_salary, subsidy, bonus, social_insurance, housing_fund FROM staff_master WHERE basic_salary IS NOT NULL LIMIT 10;
SELECT '--- distinct booking staff with revenue ---' AS info;
SELECT staff_id, COUNT(*) AS bk_cnt, SUM(CASE WHEN final_amount IS NOT NULL THEN 1 ELSE 0 END) AS has_amt, SUM(final_amount) AS total_rev, COUNT(DISTINCT booking_status) FROM booking_master WHERE staff_id IS NOT NULL GROUP BY staff_id LIMIT 25;
SELECT '--- booking status distribution ---' AS info;
SELECT booking_status, COUNT(*) FROM booking_master GROUP BY booking_status;
SELECT '--- overtime by month ---' AS info;
SELECT DATE_FORMAT(overtime_date,'%Y-%m') AS m, COUNT(*), SUM(hours) FROM overtime GROUP BY m ORDER BY m DESC LIMIT 5;
SELECT '--- attendance total_early check ---' AS info;
SELECT total_late, total_early, total_absent, total_present FROM attendance_records LIMIT 3;
