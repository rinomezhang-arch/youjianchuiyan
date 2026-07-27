package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===== 桌台管理 =====
    
    /** 获取桌台列表 */
    @GetMapping("/table/list")
    public Result<List<Map<String, Object>>> getTableList(HttpServletRequest request) {
        validateHeader(request);
        try {
            String sql = "SELECT table_id, table_name, table_area, table_status, seats, sort_order " +
                         "FROM banquet_table WHERE is_active = 1 ORDER BY sort_order";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取桌台列表失败: " + e.getMessage());
        }
    }

    /** 开台 */
    @PostMapping("/table/open")
    public Result<Map<String, Object>> openTable(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        validateHeader(request);
        try {
            String tableId = (String) body.get("table_id");
            Integer guestCount = body.get("guest_count") != null ? ((Number) body.get("guest_count")).intValue() : 0;
            String customerName = (String) body.get("customer_name");
            String customerPhone = (String) body.get("customer_phone");
            
            // 更新桌台状态为占用
            String sql1 = "UPDATE banquet_table SET table_status = 'occupied' WHERE table_id = ?";
            jdbcTemplate.update(sql1, tableId);
            
            // 创建预订记录
            String bookingId = "BK" + System.currentTimeMillis();
            String sql2 = "INSERT INTO booking_master (booking_id, store_id, booking_date, booking_status, " +
                         "guest_count, customer_name, customer_phone, booker, staff_id, created_at) " +
                         "VALUES (?, 1, CURDATE(), 'confirmed', ?, ?, ?, 'ipad', 1, NOW())";
            jdbcTemplate.update(sql2, bookingId, guestCount, customerName, customerPhone);
            
            // 创建桌台关联
            String sql3 = "INSERT INTO table_booking (booking_id, table_id) VALUES (?, ?)";
            jdbcTemplate.update(sql3, bookingId, tableId);
            
            return Result.success(Map.of("booking_id", bookingId, "table_id", tableId));
        } catch (Exception e) {
            return Result.error(500, "开台失败: " + e.getMessage());
        }
    }

    // ===== 菜品分类 =====
    
    /** 获取菜品分类（按菜单类型） */
    @GetMapping("/dish/category")
    public Result<List<Map<String, Object>>> getDishCategory(
            @RequestParam(required = false, defaultValue = "alacarte") String menu_type,
            HttpServletRequest request) {
        validateHeader(request);
        try {
            String sql = "SELECT id, category_id, category_name, category_code, sort_order, is_active, menu_type, " +
                         "(SELECT COUNT(*) FROM dish_master WHERE category_id = dc.category_id AND is_active = 1 AND menu_type LIKE CONCAT('%', ?, '%')) as dish_count " +
                         "FROM dish_category dc WHERE is_active = 1 AND menu_type LIKE CONCAT('%', ?, '%') ORDER BY sort_order";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, menu_type, menu_type);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取菜品分类失败: " + e.getMessage());
        }
    }

    // ===== 菜品列表 =====
    
    /** 获取菜品列表（按分类） */
    @GetMapping("/dish/list")
    public Result<List<Map<String, Object>>> getDishList(
            @RequestParam(required = false) String category_id,
            @RequestParam(required = false, defaultValue = "alacarte") String menu_type,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        validateHeader(request);
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT dish_id, dish_name, dish_name_en, sale_price, cost_price, image_url, ");
            sql.append("spicy_level, is_active, is_specialty, is_seasonal, cooking_method, taste, ");
            sql.append("sort_order, dish_category, category_id, menu_type ");
            sql.append("FROM dish_master WHERE is_active = 1 AND menu_type LIKE CONCAT('%', ?, '%')");
            
            if (category_id != null && !category_id.isEmpty()) {
                sql.append(" AND category_id = ?");
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (dish_name LIKE CONCAT('%', ?, '%') OR dish_name_en LIKE CONCAT('%', ?, '%'))");
            }
            sql.append(" ORDER BY sort_order");
            
            List<Map<String, Object>> list;
            if (category_id != null && !category_id.isEmpty()) {
                if (keyword != null && !keyword.isEmpty()) {
                    list = jdbcTemplate.queryForList(sql.toString(), menu_type, category_id, keyword, keyword);
                } else {
                    list = jdbcTemplate.queryForList(sql.toString(), menu_type, category_id);
                }
            } else {
                if (keyword != null && !keyword.isEmpty()) {
                    list = jdbcTemplate.queryForList(sql.toString(), menu_type, keyword, keyword);
                } else {
                    list = jdbcTemplate.queryForList(sql.toString(), menu_type);
                }
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取菜品列表失败: " + e.getMessage());
        }
    }

    /** 获取菜品详情 */
    @GetMapping("/dish/detail/{dish_id}")
    public Result<Map<String, Object>> getDishDetail(@PathVariable String dish_id, HttpServletRequest request) {
        validateHeader(request);
        try {
            String sql = "SELECT * FROM dish_master WHERE dish_id = ?";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, dish_id);
            if (list.isEmpty()) {
                return Result.error(404, "菜品不存在");
            }
            return Result.success(list.get(0));
        } catch (Exception e) {
            return Result.error(500, "获取菜品详情失败: " + e.getMessage());
        }
    }

    // ===== 订单菜品管理 =====
    
    /** 添加菜品到订单 */
    @PostMapping("/order/dish/add")
    public Result<Map<String, Object>> addDishToOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        validateHeader(request);
        try {
            String tableId = (String) body.get("table_id");
            String dishId = (String) body.get("dish_id");
            Integer quantity = body.get("quantity") != null ? ((Number) body.get("quantity")).intValue() : 1;
            String dishNote = (String) body.get("dish_note");
            String specification = (String) body.get("specification");
            
            // 获取菜品价格
            String sql1 = "SELECT sale_price, dish_name FROM dish_master WHERE dish_id = ?";
            List<Map<String, Object>> dishList = jdbcTemplate.queryForList(sql1, dishId);
            if (dishList.isEmpty()) {
                return Result.error(404, "菜品不存在");
            }
            BigDecimal salePrice = (BigDecimal) dishList.get(0).get("sale_price");
            String dishName = (String) dishList.get(0).get("dish_name");
            
            // 获取 booking_id
            String sql2 = "SELECT b.booking_id FROM booking_master b " +
                         "JOIN table_booking tb ON b.booking_id = tb.booking_id " +
                         "WHERE tb.table_id = ? AND b.booking_status = 'confirmed' ORDER BY b.created_at DESC LIMIT 1";
            List<Map<String, Object>> bookingList = jdbcTemplate.queryForList(sql2, tableId);
            String bookingId;
            if (bookingList.isEmpty()) {
                // 自动开台创建订单
                bookingId = "BK" + System.currentTimeMillis();
                String sql3 = "INSERT INTO booking_master (booking_id, store_id, booking_date, booking_status, booker, staff_id, created_at) " +
                             "VALUES (?, 1, CURDATE(), 'confirmed', 'ipad', 1, NOW())";
                jdbcTemplate.update(sql3, bookingId);
                String sql4 = "INSERT INTO table_booking (booking_id, table_id) VALUES (?, ?)";
                jdbcTemplate.update(sql4, bookingId, tableId);
            } else {
                bookingId = (String) bookingList.get(0).get("booking_id");
            }
            
            // 添加菜品到订单
            BigDecimal subtotal = salePrice.multiply(BigDecimal.valueOf(quantity));
            String sql5 = "INSERT INTO booking_dish_detail (booking_id, dish_id, dish_name, dish_quantity, " +
                         "unit_price, subtotal, dish_note, specification, kitchen_status, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', NOW())";
            jdbcTemplate.update(sql5, bookingId, dishId, dishName, quantity, salePrice, subtotal, dishNote, specification);
            
            return Result.success(Map.of("booking_id", bookingId, "dish_id", dishId, "quantity", quantity));
        } catch (Exception e) {
            return Result.error(500, "添加菜品失败: " + e.getMessage());
        }
    }

    /** 更新订单菜品 */
    @PutMapping("/order/dish/edit")
    public Result<Void> editOrderDish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        validateHeader(request);
        try {
            Integer dishBookingId = body.get("dish_booking_id") != null ? ((Number) body.get("dish_booking_id")).intValue() : 0;
            Integer quantity = body.get("quantity") != null ? ((Number) body.get("quantity")).intValue() : 0;
            String dishNote = (String) body.get("dish_note");
            
            // 获取单价重新计算小计
            String sql1 = "SELECT unit_price FROM booking_dish_detail WHERE dish_booking_id = ?";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql1, dishBookingId);
            if (list.isEmpty()) {
                return Result.error(404, "订单菜品不存在");
            }
            BigDecimal unitPrice = (BigDecimal) list.get(0).get("unit_price");
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            
            String sql2 = "UPDATE booking_dish_detail SET dish_quantity = ?, subtotal = ?, dish_note = ? WHERE dish_booking_id = ?";
            jdbcTemplate.update(sql2, quantity, subtotal, dishNote, dishBookingId);
            
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "更新菜品失败: " + e.getMessage());
        }
    }

    /** 删除订单菜品 */
    @DeleteMapping("/order/dish/remove/{dish_booking_id}")
    public Result<Void> removeOrderDish(@PathVariable Integer dish_booking_id, HttpServletRequest request) {
        validateHeader(request);
        try {
            String sql = "DELETE FROM booking_dish_detail WHERE dish_booking_id = ?";
            jdbcTemplate.update(sql, dish_booking_id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "删除菜品失败: " + e.getMessage());
        }
    }

    /** 获取桌台当前订单 */
    @GetMapping("/order/current")
    public Result<List<Map<String, Object>>> getCurrentOrder(
            @RequestParam String table_id,
            HttpServletRequest request) {
        validateHeader(request);
        try {
            String sql = "SELECT d.dish_booking_id, d.dish_id, d.dish_name, d.dish_quantity, d.unit_price, " +
                         "d.subtotal, d.dish_note, d.specification, d.kitchen_status " +
                         "FROM booking_dish_detail d " +
                         "JOIN table_booking tb ON d.booking_id = tb.booking_id " +
                         "WHERE tb.table_id = ? ORDER BY d.created_at";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, table_id);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取订单失败: " + e.getMessage());
        }
    }

    // ===== 提交后厨 =====
    
    /** 提交后厨 */
    @PostMapping("/order/send-kitchen")
    public Result<Void> sendToKitchen(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        validateHeader(request);
        try {
            String tableId = (String) body.get("table_id");
            String orderNote = (String) body.get("order_note");
            
            // 获取 booking_id
            String sql1 = "SELECT b.booking_id FROM booking_master b " +
                         "JOIN table_booking tb ON b.booking_id = tb.booking_id " +
                         "WHERE tb.table_id = ? AND b.booking_status = 'confirmed' ORDER BY b.created_at DESC LIMIT 1";
            List<Map<String, Object>> bookingList = jdbcTemplate.queryForList(sql1, tableId);
            if (bookingList.isEmpty()) {
                return Result.error(404, "未找到订单");
            }
            String bookingId = (String) bookingList.get(0).get("booking_id");
            
            // 更新菜品状态为已提交
            String sql2 = "UPDATE booking_dish_detail SET kitchen_status = 'submitted' WHERE booking_id = ? AND kitchen_status = 'pending'";
            jdbcTemplate.update(sql2, bookingId);
            
            // 更新预订备注
            if (orderNote != null) {
                String sql3 = "UPDATE booking_master SET remark = ? WHERE booking_id = ?";
                jdbcTemplate.update(sql3, orderNote, bookingId);
            }
            
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "提交后厨失败: " + e.getMessage());
        }
    }

    // ===== 工具方法 =====
    
    private void validateHeader(HttpServletRequest request) {
        String storeId = request.getHeader("X-Store-Id");
        String staffId = request.getHeader("X-Staff-Id");
        String deviceSn = request.getHeader("X-Device-Sn");
        String clientType = request.getHeader("X-Client-Type");
        
        // 允许开发阶段跳过验证
        if (storeId == null) storeId = "1";
        if (staffId == null) staffId = "1";
        if (deviceSn == null) deviceSn = "DEV";
        if (clientType == null) clientType = "ipad";
    }
}
