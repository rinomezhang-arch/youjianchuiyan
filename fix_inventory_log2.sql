ALTER TABLE ingredient_inventory_log 
MODIFY COLUMN log_type VARCHAR(20) NULL,
MODIFY COLUMN log_quantity DECIMAL(12,3) NULL;
