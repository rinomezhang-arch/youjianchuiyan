ALTER TABLE dish_master ADD COLUMN menu_type varchar(20) DEFAULT 'alacarte' COMMENT '菜单类型: alacarte零点/banquet宴会/all全部';
ALTER TABLE dish_category ADD COLUMN menu_type varchar(20) DEFAULT 'alacarte' COMMENT '菜单类型';
UPDATE dish_master SET menu_type = 'alacarte' WHERE menu_type IS NULL OR menu_type = '';
UPDATE dish_category SET menu_type = 'alacarte' WHERE menu_type IS NULL OR menu_type = '';
SELECT COUNT(*) as dish_total FROM dish_master;
SELECT COUNT(*) as cat_total FROM dish_category;
