package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.CustomerMaster;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.repository.CustomerMasterRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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

    @GetMapping("/customers")
    public Result<List<CustomerMaster>> getCustomers(@RequestParam(defaultValue = "1") Long storeId,
                                                      @RequestParam(required = false) String keyword) {
        try {
            List<CustomerMaster> list;
            if (keyword != null && !keyword.isEmpty()) {
                list = customerRepo.search(storeId, keyword);
            } else {
                list = customerRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
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
            return Result.success(customerRepo.search(storeId, keyword));
        } catch (Exception e) {
            return Result.error(500, "搜索客户失败: " + e.getMessage());
        }
    }

    @GetMapping("/customers/{id}")
    public Result<CustomerMaster> getCustomerById(@PathVariable Integer id) {
        try {
            CustomerMaster c = customerRepo.findById(id).orElse(null);
            if (c == null) return Result.error(404, "客户不存在");
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
            customer.setCustomerId(null);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());
            if (customer.getIsActive() == null) customer.setIsActive(1);
            if (customer.getTotalAmount() == null) customer.setTotalAmount(BigDecimal.ZERO);
            if (customer.getBookingCount() == null) customer.setBookingCount(0);
            CustomerMaster saved = customerRepo.save(customer);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "创建客户失败: " + e.getMessage());
        }
    }

    @PutMapping("/customers/{id}")
    @Transactional
    public Result<CustomerMaster> updateCustomer(@PathVariable Integer id, @RequestBody CustomerMaster customer) {
        try {
            CustomerMaster existing = customerRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "客户不存在");
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
            CustomerMaster existing = customerRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "客户不存在");
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
