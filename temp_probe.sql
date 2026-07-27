SELECT 'staff_active' AS tbl, COUNT(*) AS c FROM staff_master WHERE employment_status='active' OR employment_status IS NULL
UNION SELECT 'attendance_records', COUNT(*) FROM attendance_records
UNION SELECT 'leave_record', COUNT(*) FROM leave_record
UNION SELECT 'overtime', COUNT(*) FROM overtime
UNION SELECT 'booking_master', COUNT(*) FROM booking_master
UNION SELECT 'report_staff_kpi', COUNT(*) FROM report_staff_kpi;
SELECT '--- attendance months ---' AS info;
SELECT month, COUNT(*) FROM attendance_records GROUP BY month ORDER BY month DESC LIMIT 5;
SELECT '--- booking range ---' AS info;
SELECT MIN(booking_date), MAX(booking_date), COUNT(DISTINCT staff_id) FROM booking_master;
SELECT '--- staff departments ---' AS info;
SELECT department, COUNT(*) FROM staff_master WHERE employment_status='active' OR employment_status IS NULL GROUP BY department;
SELECT '--- employment status ---' AS info;
SELECT employment_status, COUNT(*) FROM staff_master GROUP BY employment_status;
