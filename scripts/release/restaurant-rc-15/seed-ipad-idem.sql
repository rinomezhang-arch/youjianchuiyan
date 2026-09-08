-- RC15 iPad batch idempotency real-port seed (ASCII, idempotent).
-- Isolated DB only: banquet_rc15 on 127.0.0.1:13317. Never run against production.
-- Prereqs: scripts/migrations/ipad_device_binding_migration_v1.sql (FK targets fixed
-- to store_info(store_id) / staff_master(staff_id)) and classpath
-- ipad_batch_request_migration_v1.sql already applied.

-- Rerun cleanup (reverse FK order)
DELETE FROM booking_dish_detail WHERE booking_id = 'RC15-IDEM-BK' AND store_id = 1;
DELETE FROM ipad_batch_request   WHERE booking_id = 'RC15-IDEM-BK' AND store_id = 1;
DELETE FROM dish_master          WHERE dish_id = 'RC15-IDEM-DISH' AND store_id = 1;
DELETE FROM booking_master       WHERE booking_id = 'RC15-IDEM-BK' AND store_id = 1;
DELETE FROM ipad_device_binding  WHERE device_sn = 'RC15IDEMDEV';
DELETE FROM staff_master         WHERE staff_id = 405;

-- Synthetic iPad staff, store 1, plaintext test password (isolated DB only)
INSERT INTO staff_master (staff_id, store_id, staff_account, staff_password, staff_name, role, employment_status)
VALUES (405, 1, 'syn_ipad', 'synpass123', 'Syn Ipad', 'staff', 'active');

-- Device binding
INSERT INTO ipad_device_binding (device_sn, store_id, staff_id, device_name, status)
VALUES ('RC15IDEMDEV', 1, 405, 'RC15 Idem iPad', 'active');

-- Confirmed booking
INSERT INTO booking_master (booking_id, store_id, booking_status, payment_status)
VALUES ('RC15-IDEM-BK', 1, 'confirmed', 'unpaid');

-- Active menu dish
INSERT INTO dish_master (dish_id, store_id, dish_name, sale_price, is_active)
VALUES ('RC15-IDEM-DISH', 1, 'RC15 Idem Dish', 12.50, 1);
