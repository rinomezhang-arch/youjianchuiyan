/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.CustomerDTO
 *  com.youjian.banquet.entity.CustomerMaster
 *  com.youjian.banquet.repository.CustomerMasterRepository
 *  com.youjian.banquet.service.CustomerService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.CustomerDTO;
import com.youjian.banquet.entity.CustomerMaster;
import com.youjian.banquet.repository.CustomerMasterRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    @Autowired
    private CustomerMasterRepository customerMasterRepository;

    public List<CustomerDTO> getAllCustomers(String storeId) {
        return this.customerMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public CustomerDTO getCustomer(String customerId, String storeId) {
        return this.customerMasterRepository.findByCustomerIdAndStoreId(Integer.valueOf(Integer.parseInt(customerId)), Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    public List<CustomerDTO> searchCustomers(String storeId, String keyword) {
        return this.customerMasterRepository.searchByKeyword(Long.valueOf(Long.parseLong(storeId)), keyword).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerDTO dto) {
        CustomerMaster customer = new CustomerMaster();
        customer.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        customer.setCustomerName(dto.getCustomerName());
        customer.setCustomerPhone(dto.getPhone());
        customer.setCustomerPreference(dto.getNotes());
        customer.setTotalAmount(dto.getTotalSpent() != null ? dto.getTotalSpent() : BigDecimal.ZERO);
        customer.setBookingCount(Integer.valueOf(dto.getTotalVisits() != null ? dto.getTotalVisits() : 0));
        customer.setIsActive(Integer.valueOf("active".equals(dto.getStatus()) ? 1 : 0));
        customer.setRemark(dto.getNotes());
        this.customerMasterRepository.save(customer);
        return this.toDTO(customer);
    }

    @Transactional
    public CustomerDTO updateCustomer(String customerId, String storeId, CustomerDTO dto) {
        CustomerMaster customer = (CustomerMaster)this.customerMasterRepository.findByCustomerIdAndStoreId(Integer.valueOf(Integer.parseInt(customerId)), Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        if (dto.getCustomerName() != null) {
            customer.setCustomerName(dto.getCustomerName());
        }
        if (dto.getPhone() != null) {
            customer.setCustomerPhone(dto.getPhone());
        }
        if (dto.getNotes() != null) {
            customer.setCustomerPreference(dto.getNotes());
        }
        if (dto.getTotalSpent() != null) {
            customer.setTotalAmount(dto.getTotalSpent());
        }
        if (dto.getTotalVisits() != null) {
            customer.setBookingCount(dto.getTotalVisits());
        }
        if (dto.getStatus() != null) {
            customer.setIsActive(Integer.valueOf("active".equals(dto.getStatus()) ? 1 : 0));
        }
        if (dto.getNotes() != null) {
            customer.setRemark(dto.getNotes());
        }
        this.customerMasterRepository.save(customer);
        return this.toDTO(customer);
    }

    @Transactional
    public void deleteCustomer(String customerId, String storeId) {
        this.customerMasterRepository.findByCustomerIdAndStoreId(Integer.valueOf(Integer.parseInt(customerId)), Long.valueOf(Long.parseLong(storeId))).ifPresent(arg_0 -> ((CustomerMasterRepository)this.customerMasterRepository).delete(arg_0));
    }

    private CustomerDTO toDTO(CustomerMaster e) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(String.valueOf(e.getCustomerId()));
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setCustomerName(e.getCustomerName());
        dto.setPhone(e.getCustomerPhone());
        dto.setNotes(e.getCustomerPreference());
        dto.setTotalVisits(e.getBookingCount());
        dto.setTotalSpent(e.getTotalAmount());
        dto.setStatus(e.getIsActive() != null && e.getIsActive() == 1 ? "active" : "inactive");
        return dto;
    }
}

