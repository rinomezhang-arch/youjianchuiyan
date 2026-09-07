-- Restaurant-only widening: retain the actual latest receipt price through requisitions and logs.
-- Existing columns are nullable; widening preserves historical values and integer capacity.
-- Apply after schema comparison and backup; never run automatically against production.
ALTER TABLE material_requisition_item MODIFY COLUMN unit_price DECIMAL(18,8) NULL;
ALTER TABLE ingredient_inventory_log MODIFY COLUMN unit_price DECIMAL(15,8) NULL;
