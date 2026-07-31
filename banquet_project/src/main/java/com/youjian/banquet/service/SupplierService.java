/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.SupplierDTO
 *  com.youjian.banquet.entity.SupplierMaster
 *  com.youjian.banquet.repository.SupplierMasterRepository
 *  com.youjian.banquet.service.SupplierService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.SupplierDTO;
import com.youjian.banquet.entity.SupplierMaster;
import com.youjian.banquet.repository.SupplierMasterRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
    @Autowired
    private SupplierMasterRepository supplierMasterRepository;

    public List<SupplierDTO> getAllSuppliers(String storeId) {
        return this.supplierMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public SupplierDTO getSupplier(String supplierId, String storeId) {
        return this.supplierMasterRepository.findBySupplierIdAndStoreId(Long.valueOf(Long.parseLong(supplierId)), Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto) {
        SupplierMaster supplier = new SupplierMaster();
        supplier.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        supplier.setSupplierName(dto.getSupplierName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setCategory(dto.getCategory());
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        supplier.setNotes(dto.getNotes());
        this.supplierMasterRepository.save(supplier);
        return this.toDTO(supplier);
    }

    @Transactional
    public SupplierDTO updateSupplier(String supplierId, String storeId, SupplierDTO dto) {
        SupplierMaster supplier = (SupplierMaster)this.supplierMasterRepository.findBySupplierIdAndStoreId(Long.valueOf(Long.parseLong(supplierId)), Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        if (dto.getSupplierName() != null) {
            supplier.setSupplierName(dto.getSupplierName());
        }
        if (dto.getContactPerson() != null) {
            supplier.setContactPerson(dto.getContactPerson());
        }
        if (dto.getPhone() != null) {
            supplier.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            supplier.setEmail(dto.getEmail());
        }
        if (dto.getAddress() != null) {
            supplier.setAddress(dto.getAddress());
        }
        if (dto.getCategory() != null) {
            supplier.setCategory(dto.getCategory());
        }
        if (dto.getPaymentTerms() != null) {
            supplier.setPaymentTerms(dto.getPaymentTerms());
        }
        if (dto.getStatus() != null) {
            supplier.setStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            supplier.setNotes(dto.getNotes());
        }
        this.supplierMasterRepository.save(supplier);
        return this.toDTO(supplier);
    }

    @Transactional
    public void deleteSupplier(String supplierId, String storeId) {
        this.supplierMasterRepository.findBySupplierIdAndStoreId(Long.valueOf(Long.parseLong(supplierId)), Long.valueOf(Long.parseLong(storeId))).ifPresent(arg_0 -> ((SupplierMasterRepository)this.supplierMasterRepository).delete(arg_0));
    }

    private SupplierDTO toDTO(SupplierMaster e) {
        SupplierDTO dto = new SupplierDTO();
        dto.setSupplierId(String.valueOf(e.getSupplierId()));
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setSupplierName(e.getSupplierName());
        dto.setContactPerson(e.getContactPerson());
        dto.setPhone(e.getPhone());
        dto.setEmail(e.getEmail());
        dto.setAddress(e.getAddress());
        dto.setCategory(e.getCategory());
        dto.setPaymentTerms(e.getPaymentTerms());
        dto.setStatus(e.getStatus());
        dto.setNotes(e.getNotes());
        return dto;
    }
}

