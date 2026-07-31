/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.IngredientDTO
 *  com.youjian.banquet.entity.IngredientMaster
 *  com.youjian.banquet.repository.IngredientMasterRepository
 *  com.youjian.banquet.repository.SupplierMasterRepository
 *  com.youjian.banquet.service.IngredientService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.IngredientDTO;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.SupplierMasterRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {
    @Autowired
    private IngredientMasterRepository ingredientMasterRepository;
    @Autowired
    private SupplierMasterRepository supplierMasterRepository;

    public List<IngredientDTO> getAllIngredients(String storeId) {
        return this.ingredientMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public List<IngredientDTO> getIngredientsByStatus(String storeId, String status) {
        return this.ingredientMasterRepository.findByStoreIdAndStatus(Long.valueOf(Long.parseLong(storeId)), status).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public IngredientDTO getIngredient(String ingredientId, String storeId) {
        return this.ingredientMasterRepository.findByIngredientIdAndStoreId(ingredientId, Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + ingredientId));
    }

    public List<IngredientDTO> getLowStockIngredients(String storeId) {
        return this.ingredientMasterRepository.findLowStockIngredients(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    @Transactional
    public IngredientDTO createIngredient(IngredientDTO dto) {
        IngredientMaster ingredient = new IngredientMaster();
        ingredient.setIngredientId((String)(dto.getIngredientId() != null ? dto.getIngredientId() : "ING-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()));
        ingredient.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        ingredient.setIngredientName(dto.getIngredientName());
        ingredient.setCategory(dto.getCategory());
        ingredient.setUnit(dto.getUnit());
        ingredient.setCurrentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO);
        ingredient.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : BigDecimal.ZERO);
        ingredient.setUnitPrice(dto.getUnitPrice());
        ingredient.setSupplierId(dto.getSupplierId() != null ? Long.valueOf(Long.parseLong(dto.getSupplierId())) : null);
        ingredient.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        this.ingredientMasterRepository.save(ingredient);
        return this.toDTO(ingredient);
    }

    @Transactional
    public IngredientDTO updateIngredient(String ingredientId, String storeId, IngredientDTO dto) {
        IngredientMaster ingredient = (IngredientMaster)this.ingredientMasterRepository.findByIngredientIdAndStoreId(ingredientId, Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Ingredient not found: " + ingredientId));
        if (dto.getIngredientName() != null) {
            ingredient.setIngredientName(dto.getIngredientName());
        }
        if (dto.getCategory() != null) {
            ingredient.setCategory(dto.getCategory());
        }
        if (dto.getUnit() != null) {
            ingredient.setUnit(dto.getUnit());
        }
        if (dto.getCurrentStock() != null) {
            ingredient.setCurrentStock(dto.getCurrentStock());
        }
        if (dto.getMinStock() != null) {
            ingredient.setMinStock(dto.getMinStock());
        }
        if (dto.getUnitPrice() != null) {
            ingredient.setUnitPrice(dto.getUnitPrice());
        }
        if (dto.getSupplierId() != null) {
            ingredient.setSupplierId(Long.valueOf(Long.parseLong(dto.getSupplierId())));
        }
        if (dto.getStatus() != null) {
            ingredient.setStatus(dto.getStatus());
        }
        this.ingredientMasterRepository.save(ingredient);
        return this.toDTO(ingredient);
    }

    @Transactional
    public void deleteIngredient(String ingredientId, String storeId) {
        this.ingredientMasterRepository.deleteByIngredientIdAndStoreId(ingredientId, Long.valueOf(Long.parseLong(storeId)));
    }

    private IngredientDTO toDTO(IngredientMaster e) {
        IngredientDTO dto = new IngredientDTO();
        dto.setIngredientId(e.getIngredientId());
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setIngredientName(e.getIngredientName());
        dto.setCategory(e.getCategory());
        dto.setUnit(e.getUnit());
        dto.setCurrentStock(e.getCurrentStock());
        dto.setMinStock(e.getMinStock());
        dto.setUnitPrice(e.getUnitPrice());
        dto.setSupplierId(e.getSupplierId() != null ? String.valueOf(e.getSupplierId()) : null);
        dto.setStatus(e.getStatus());
        if (e.getSupplierId() != null) {
            this.supplierMasterRepository.findBySupplierIdAndStoreId(e.getSupplierId(), e.getStoreId()).ifPresent(s -> dto.setSupplierName(s.getSupplierName()));
        }
        return dto;
    }
}

