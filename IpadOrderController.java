package com.youjian.banquet.ipad.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingMasterId;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.KitchenLog;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.KitchenLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ipad")
@CrossOrigin(origins = "*")
public class IpadOrderController {

    @Autowired
    private BookingDishDetailRepository dishDetailRepo;

    @Autowired
    private BookingMasterRepository bookingRepo;

    @Autowired
    private BookingTableRepository bookingTableRepo;

    @Autowired
    private DishMasterRepository dishRepo;

    @Autowired
    private KitchenLogRepository kitchenLogRepo;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @GetMapping("/order/current")
    public Result<List<Map<String, Object>>> getCurrentOrder(
            @RequestParam String table_id,
            HttpServletRequest request) {
        try {
            String sql = "SELECT d.dish_booking_id, d.dish_id, d.dish_name, d.dish_quantity, " +
                         "d.unit_price, d.subtotal, d.dish_note, d.kitchen_status " +
                         "FROM booking_dish_detail d " +
                         "JOIN booking_table bt ON d.booking_id = bt.booking_id " +
                         "WHERE bt.table_id = ? " +
                         "ORDER BY d.created_at DESC";
            List<Map<String, Object>> list = entityManager.createNativeQuery(sql)
                    .setParameter(1, Integer.parseInt(table_id))
                    .unwrap(org.hibernate.query.NativeQuery.class)
                    .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE)
                    .list();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取订单失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/dish/add")
    public Result<Map<String, Object>> addDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) storeId = 1L;

            String tableId = (String) body.get("table_id");
            String bookingId = (String) body.get("booking_id");
            String dishId = (String) body.get("dish_id");
            
            Integer dishQuantity = 1;
            Object qtyObj = body.get("dish_quantity");
            if (qtyObj instanceof Number) {
                dishQuantity = ((Number) qtyObj).intValue();
            } else if (qtyObj != null) {
                dishQuantity = Integer.parseInt(qtyObj.toString());
            }
            
            String dishNote = (String) body.get("dish_note");

            if (tableId != null && bookingId == null) {
                String findBookingSql = "SELECT booking_id FROM booking_table WHERE table_id = ?";
                List<?> bookingResults = entityManager.createNativeQuery(findBookingSql)
                        .setParameter(1, Integer.parseInt(tableId))
                        .getResultList();
                if (!bookingResults.isEmpty()) {
                    bookingId = (String) bookingResults.get(0);
                }

                if (bookingId == null) {
                    String newBookingId = "BK" + System.currentTimeMillis();
                    BookingMaster booking = new BookingMaster();
                    booking.setBookingId(newBookingId);
                    booking.setStoreId(storeId);
                    booking.setBookingDate(LocalDate.now());
                    booking.setBookingTime(LocalTime.now());
                    booking.setBookingStatus("confirmed");
                    booking.setStaffId(1);
                    bookingRepo.save(booking);

                    BookingTable bookingTable = new BookingTable();
                    bookingTable.setBookingId(newBookingId);
                    bookingTable.setStoreId(storeId);
                    bookingTable.setBookingDate(LocalDate.now());
                    bookingTable.setBookingTime(LocalTime.now());
                    bookingTable.setTableId(Integer.parseInt(tableId));
                    bookingTable.setGuestCount(0);
                    bookingTableRepo.save(bookingTable);

                    bookingId = newBookingId;
                }
            }

            if (bookingId == null) {
                return Result.error(400, "缺少 booking_id 或 table_id");
            }

            if (dishId == null || dishId.isEmpty()) {
                return Result.error(400, "缺少 dish_id");
            }

            DishMaster dish = dishRepo.findByDishIdAndStoreId(dishId, storeId)
                    .orElseThrow(() -> new RuntimeException("菜品不存在"));

            BigDecimal unitPrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(dishQuantity));

            BookingDishDetail detail = new BookingDishDetail();
            detail.setStoreId(storeId);
            detail.setBookingId(bookingId);
            detail.setDishId(dishId);
            detail.setDishName(dish.getDishName());
            detail.setDishQuantity(dishQuantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);
            detail.setDishNote(dishNote);
            detail.setKitchenStatus("pending");
            detail.setCreatedAt(LocalDateTime.now());

            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", detail.getDishBookingId());
            data.put("booking_id", bookingId);
            data.put("dish_id", dishId);
            data.put("dish_name", dish.getDishName());
            data.put("dish_quantity", dishQuantity);
            data.put("unit_price", unitPrice);
            data.put("subtotal", subtotal);
            data.put("kitchen_status", "pending");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加菜失败：" + e.getMessage());
        }
    }

    @PutMapping("/order/dish/edit")
    public Result<Map<String, Object>> editDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");

            Integer dishBookingId = Integer.parseInt(body.get("dish_booking_id").toString());
            Integer dishQuantity = Integer.parseInt(body.get("dish_quantity").toString());
            String dishNote = (String) body.get("dish_note");

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

            BigDecimal subtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(dishQuantity));

            detail.setDishQuantity(dishQuantity);
            detail.setDishNote(dishNote);
            detail.setSubtotal(subtotal);

            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", detail.getDishBookingId());
            data.put("dish_quantity", dishQuantity);
            data.put("subtotal", subtotal);
            data.put("dish_note", dishNote);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "改菜失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/order/dish/remove")
    public Result<Map<String, Object>> removeDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Integer dishBookingId = Integer.parseInt(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

            dishDetailRepo.delete(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "删菜失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/dish/refund")
    public Result<Map<String, Object>> refundDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Integer dishBookingId = Integer.parseInt(body.get("dish_booking_id").toString());
            String refundReason = (String) body.get("refund_reason");

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

            detail.setKitchenStatus("refunded");
            detail.setDishNote(refundReason);

            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);
            data.put("kitchen_status", "refunded");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "退菜失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/send-kitchen")
    public Result<Map<String, Object>> sendToKitchen(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long storeId = (Long) request.getAttribute("ipad_store_id");
            if (storeId == null) storeId = 1L;
            Long staffId = (Long) request.getAttribute("ipad_staff_id");

            String bookingId = (String) body.get("booking_id");
            String tableId = (String) body.get("table_id");

            if (tableId != null && bookingId == null) {
                String findBookingSql = "SELECT booking_id FROM booking_table WHERE table_id = ?";
                List<?> bookingResults = entityManager.createNativeQuery(findBookingSql)
                        .setParameter(1, Integer.parseInt(tableId))
                        .getResultList();
                if (!bookingResults.isEmpty()) {
                    bookingId = (String) bookingResults.get(0);
                }
            }

            if (bookingId == null) {
                return Result.error(400, "缺少 booking_id 或 table_id");
            }

            List<BookingDishDetail> pendingDishes = dishDetailRepo.findByBookingId(bookingId).stream()
                    .filter(d -> "pending".equals(d.getKitchenStatus()))
                    .collect(Collectors.toList());

            if (pendingDishes.isEmpty()) {
                return Result.error(400, "没有待处理的菜品");
            }

            for (BookingDishDetail detail : pendingDishes) {
                detail.setKitchenStatus("submitted");
                dishDetailRepo.save(detail);

                KitchenLog log = new KitchenLog();
                log.setBookingId(bookingId);
                log.setDishId(detail.getDishId());
                log.setStoreId(storeId);
                log.setCreatedAt(LocalDateTime.now());
                kitchenLogRepo.save(log);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", bookingId);
            data.put("dish_count", pendingDishes.size());
            data.put("kitchen_status", "submitted");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "提交后厨失败：" + e.getMessage());
        }
    }

    @PostMapping("/order/urgent")
    public Result<Map<String, Object>> urgentDish(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Integer dishBookingId = Integer.parseInt(body.get("dish_booking_id").toString());

            BookingDishDetail detail = dishDetailRepo.findById(dishBookingId)
                    .orElseThrow(() -> new RuntimeException("菜品明细不存在"));

            detail.setKitchenStatus("urgent");
            detail = dishDetailRepo.save(detail);

            Map<String, Object> data = new HashMap<>();
            data.put("dish_booking_id", dishBookingId);
            data.put("kitchen_status", "urgent");

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加急失败：" + e.getMessage());
        }
    }
}