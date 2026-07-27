ALTER TABLE ingredient_inventory_log 
ADD COLUMN change_type VARCHAR(50) AFTER ingredient_id,
ADD COLUMN quantity DECIMAL(10,3) AFTER change_type,
ADD COLUMN before_stock DECIMAL(10,3) AFTER quantity,
ADD COLUMN after_stock DECIMAL(10,3) AFTER before_stock,
ADD COLUMN reference_id VARCHAR(100) AFTER after_stock,
ADD COLUMN reference_type VARCHAR(50) AFTER reference_id,
ADD COLUMN operator VARCHAR(100) AFTER reference_type,
ADD COLUMN notes TEXT AFTER operator,
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER notes;
