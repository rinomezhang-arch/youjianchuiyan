package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.CustomerMaster;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.repository.CustomerMasterRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired private CustomerMasterRepository customerRepo;
    @Autowired private BookingMasterRepository bookingRepo;
    @Autowired private JdbcTemplate jdbc;

    /**
     * 查询接口门店过滤：店长强制查询本店，总经理可查询任意门店。
     * <p>GET 请求由 {@code StoreDataScopeAspect} 已填充 UserContext 并设置 dataScopeAll 标记，
     * 本方法据此覆盖客户端传入的 storeId，防止店长越权查询其他门店客户数据。
     */
    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    @GetMapping("/customers")
    public Result<List<CustomerMaster>> getCustomers(@RequestParam(defaultValue = "1") Long storeId,
                                                      @RequestParam(required = false) String keyword) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<CustomerMaster> list;
            if (keyword != null && !keyword.isEmpty()) {
                list = customerRepo.searchByKeyword(storeId, keyword);
            } else {
                list = customerRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取客户列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/customers/search")
    public Result<List<CustomerMaster>> searchCustomers(@RequestParam(defaultValue = "1") Long storeId,
                                                         @RequestParam String keyword) {
        try {
            storeId = resolveQueryStoreId(storeId);
            return Result.success(customerRepo.searchByKeyword(storeId, keyword));
        } catch (Exception e) {
            return Result.error(500, "搜索客户失败: " + e.getMessage());
        }
    }

    @GetMapping("/customers/{id}")
    public Result<CustomerMaster> getCustomerById(@PathVariable Integer id) {
        try {
            CustomerMaster c = customerRepo.findById(id).orElse(null);
            if (c == null) return Result.error(404, "客户不存在");
            // 门店数据隔离：店长仅可查看本店客户，总经理可查看任意门店
            if (c.getStoreId() != null) {
                try {
                    UserContext.assertStoreAccess(c.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可查看本店客户");
                }
            }
            return Result.success(c);
        } catch (Exception e) {
            return Result.error(500, "获取客户失败: " + e.getMessage());
        }
    }

    @GetMapping("/customers/{id}/history")
    public Result<Map<String, Object>> getCustomerHistory(@PathVariable Integer id,
                                                           @RequestParam(defaultValue = "1") Long storeId) {
        try {
            CustomerMaster c = customerRepo.findById(id).orElse(null);
            if (c == null) return Result.error(404, "客户不存在");
            // 门店数据隔离：店长仅可查看本店客户历史，总经理可查看任意门店
            if (c.getStoreId() != null) {
                try {
                    UserContext.assertStoreAccess(c.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可查看本店客户历史");
                }
            }
            storeId = resolveQueryStoreId(storeId);
            List<BookingMaster> bookings = bookingRepo.findByCustomerPhoneAndStoreId(c.getCustomerPhone(), storeId);
            Map<String, Object> result = new HashMap<>();
            result.put("customer", c);
            result.put("bookings", bookings != null ? bookings : Collections.emptyList());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取客户历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/customers")
    @Transactional
    public Result<CustomerMaster> createCustomer(@RequestBody CustomerMaster customer) {
        try {
            // 写操作需先依据 storeId 兜底设置 dataScopeAll 标记（AuditLogAspect 仅填充用户身份未设置标记）
            UserContext.ensureDataScopeFromStoreId();
            // 门店数据隔离：非GM强制使用当前门店，防止店长伪造 storeId 为其他门店创建客户
            if (!UserContext.isDataScopeAll()) {
                customer.setStoreId(UserContext.currentStoreId());
            }
            // GM 使用请求体中的 storeId

            customer.setCustomerId(null);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());
            if (customer.getIsActive() == null) customer.setIsActive(1);
            if (customer.getTotalAmount() == null) customer.setTotalAmount(BigDecimal.ZERO);
            if (customer.getBookingCount() == null) customer.setBookingCount(0);

            // 检查唯一约束 uk_store_name_phone (store_id, customer_name, customer_phone)
            // 若已存在相同记录，直接返回已有客户（幂等/upsert 语义），避免 DuplicateKeyException 导致事务回滚
            if (customer.getStoreId() != null && customer.getCustomerName() != null && customer.getCustomerPhone() != null) {
                List<Map<String, Object>> existing = jdbc.queryForList(
                    "SELECT customer_id FROM customer_master WHERE store_id=? AND customer_name=? AND customer_phone=? LIMIT 1",
                    customer.getStoreId(), customer.getCustomerName(), customer.getCustomerPhone());
                if (!existing.isEmpty()) {
                    Integer existingId = Integer.valueOf(existing.get(0).get("customer_id").toString());
                    CustomerMaster existingCustomer = customerRepo.findById(existingId).orElse(null);
                    if (existingCustomer != null) {
                        return Result.success(existingCustomer);
                    }
                }
            }

            CustomerMaster saved = customerRepo.save(customer);
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建客户失败: " + e.getMessage());
        }
    }

    @PutMapping("/customers/{id}")
    @Transactional
    public Result<CustomerMaster> updateCustomer(@PathVariable Integer id, @RequestBody CustomerMaster customer) {
        try {
            // 写操作需先依据 storeId 兜底设置 dataScopeAll 标记
            UserContext.ensureDataScopeFromStoreId();
            CustomerMaster existing = customerRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "客户不存在");
            // 门店数据隔离：店长仅可更新本店客户，GM可更新任意门店
            if (!UserContext.isDataScopeAll()) {
                try {
                    UserContext.assertStoreAccess(existing.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可更新本店客户");
                }
            }
            if (customer.getCustomerName() != null) existing.setCustomerName(customer.getCustomerName());
            if (customer.getCustomerPhone() != null) existing.setCustomerPhone(customer.getCustomerPhone());
            if (customer.getCustomerPreference() != null) existing.setCustomerPreference(customer.getCustomerPreference());
            if (customer.getRemark() != null) existing.setRemark(customer.getRemark());
            if (customer.getMemberLevel() != null) existing.setMemberLevel(customer.getMemberLevel());
            existing.setUpdatedAt(LocalDateTime.now());
            CustomerMaster saved = customerRepo.save(existing);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "更新客户失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/customers/{id}")
    @Transactional
    public Result<?> deleteCustomer(@PathVariable Integer id) {
        try {
            // 写操作需先依据 storeId 兜底设置 dataScopeAll 标记
            UserContext.ensureDataScopeFromStoreId();
            CustomerMaster existing = customerRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "客户不存在");
            // 门店数据隔离：店长仅可软删本店客户，GM可软删任意门店
            if (!UserContext.isDataScopeAll()) {
                try {
                    UserContext.assertStoreAccess(existing.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可删除本店客户");
                }
            }
            // 软删除：设为不活跃
            existing.setIsActive(0);
            existing.setUpdatedAt(LocalDateTime.now());
            customerRepo.save(existing);
            return Result.success("客户已停用");
        } catch (Exception e) {
            return Result.error(500, "删除客户失败: " + e.getMessage());
        }
    }
}
