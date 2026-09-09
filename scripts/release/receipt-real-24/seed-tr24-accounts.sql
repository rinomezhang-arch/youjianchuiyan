-- Legacy filename retained; replay is now SELECT-only. No account creation or replacement.
SELECT COUNT(*) AS existing_fixture_accounts FROM staff_master WHERE
(staff_account='coprint23_manager' AND store_id=1) OR
(staff_account='tr24_gm' AND store_id=1) OR
(staff_account='tr24_staff1' AND store_id=1) OR
(staff_account='tr24_staff2' AND store_id=2);
