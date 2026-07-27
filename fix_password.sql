UPDATE staff_master SET staff_password='002323' WHERE staff_account='rino' OR staff_account='张婧';
SELECT staff_id, staff_name, staff_account, staff_password FROM staff_master WHERE staff_account IN ('rino','张婧');
