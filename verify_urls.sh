#!/bin/bash
echo "=== 验证1: 有图片URL的菜品数 ==="
mysql -h127.0.0.1 -urino -pWo002323 banquet -e "SELECT COUNT(*) as has_image FROM dish_master WHERE is_active=1 AND image_url IS NOT NULL AND image_url != '';" 2>/dev/null

echo ""
echo "=== 验证2: 无图片URL的菜品数 ==="
mysql -h127.0.0.1 -urino -pWo002323 banquet -e "SELECT COUNT(*) as no_image FROM dish_master WHERE is_active=1 AND (image_url IS NULL OR image_url = '');" 2>/dev/null

echo ""
echo "=== 示例: 5个菜品的图片URL ==="
mysql -h127.0.0.1 -urino -pWo002323 banquet -e "SELECT dish_id, dish_name, LEFT(image_url, 100) as image_url_preview FROM dish_master WHERE is_active=1 LIMIT 5;" 2>/dev/null

echo ""
echo "=== 示例: 完整URL（1个菜品）==="
mysql -h127.0.0.1 -urino -pWo002323 banquet -e "SELECT dish_id, dish_name, image_url FROM dish_master WHERE is_active=1 LIMIT 1\G" 2>/dev/null
