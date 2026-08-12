package com.youjian.banquet.service;

import com.youjian.banquet.dto.PurchaseDTO;
import com.youjian.banquet.entity.IngredientPurchase;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.IngredientPurchaseRepository;
import com.youjian.banquet.repository.SupplierMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    @Autowired
    private IngredientPurchaseRepository purchaseRepository;
    @Autowired
    private IngredientMasterRepository ingredientMasterRepository;
    @Autowired
    private SupplierMasterRepository supplierMasterRepository;

    public List<PurchaseDTO> getAllPurchases(String storeId) {
        return purchaseRepository.findByStoreId(Long.parseLong(storeId))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PurchaseDTO> getPurchasesByStatus(String storeId, String status) {
        return purchaseRepository.findByStoreIdAndStatus(Long.parseLong(storeId), status)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PurchaseDTO getPurchase(Long purchaseId) {
        return purchaseRepository.findById(purchaseId).map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
    }

    public List<PurchaseDTO> getPurchasesByDateRange(String storeId, LocalDate start, LocalDate end) {
        return purchaseRepository.findByStoreIdAndPurchaseDateBetween(Long.parseLong(storeId), start, end)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PurchaseDTO createPurchase(PurchaseDTO dto) {
        IngredientPurchase purchase = new IngredientPurchase();
        purchase.setStoreId(Long.parseLong(dto.getStoreId()));
        purchase.setIngredientId(dto.getIngredientId());
        if (dto.getSupplierId() != null) {
            purchase.setSupplierId(Integer.parseInt(dto.getSupplierId()));
        }
        // 同时写入新字段和旧字段
        BigDecimal qty = dto.getQuantity() != null ? dto.getQuantity() : BigDecimal.ZERO;
        BigDecimal price = dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal total = dto.getTotalAmount() != null ? dto.getTotalAmount() : qty.multiply(price);

        purchase.setPurchaseQuantity(qty);
        purchase.setPurchasePrice(price);
        purchase.setPurchaseTotal(total);
        purchase.setQuantity(qty);
        purchase.setUnitPrice(price);
        purchase.setTotalAmount(total);

        purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now());
        purchase.setStatus(dto.getStatus() != null ? dto.getStatus() : "pending");
        purchase.setNotes(dto.getNotes());
        purchaseRepository.save(purchase);
        return toDTO(purchase);
    }

    @Transactional
    public PurchaseDTO updatePurchase(Long purchaseId, PurchaseDTO dto) {
        IngredientPurchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
        if (dto.getIngredientId() != null) {
            purchase.setIngredientId(dto.getIngredientId());
        }
        if (dto.getSupplierId() != null) {
            purchase.setSupplierId(Integer.parseInt(dto.getSupplierId()));
        }
        if (dto.getQuantity() != null) {
            purchase.setPurchaseQuantity(dto.getQuantity());
            purchase.setQuantity(dto.getQuantity());
        }
        if (dto.getUnitPrice() != null) {
            purchase.setPurchasePrice(dto.getUnitPrice());
            purchase.setUnitPrice(dto.getUnitPrice());
        }
        if (dto.getTotalAmount() != null) {
            purchase.setPurchaseTotal(dto.getTotalAmount());
            purchase.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getPurchaseDate() != null) {
            purchase.setPurchaseDate(dto.getPurchaseDate());
        }
        if (dto.getStatus() != null) {
            purchase.setStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            purchase.setNotes(dto.getNotes());
        }
        purchaseRepository.save(purchase);
        return toDTO(purchase);
    }

    @Transactional
    public PurchaseDTO approvePurchase(Long purchaseId, String approvedBy) {
        IngredientPurchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
        purchase.setStatus("approved");
        purchase.setApprovedBy(approvedBy);
        purchase.setApprovedAt(LocalDateTime.now());
        purchaseRepository.save(purchase);
        return toDTO(purchase);
    }

    @Transactional
    public void deletePurchase(Long purchaseId) {
        purchaseRepository.deleteById(purchaseId);
    }

    private PurchaseDTO toDTO(IngredientPurchase e) {
        PurchaseDTO dto = new PurchaseDTO();
        dto.setPurchaseId(e.getPurchaseId());
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setIngredientId(e.getIngredientId());
        dto.setSupplierId(e.getSupplierId() != null ? String.valueOf(e.getSupplierId()) : null);

        // 优先取新字段，回退旧字段
        BigDecimal qty = e.getPurchaseQuantity() != null ? e.getPurchaseQuantity() : e.getQuantity();
        BigDecimal price = e.getPurchasePrice() != null ? e.getPurchasePrice() : e.getUnitPrice();
        BigDecimal total = e.getPurchaseTotal() != null ? e.getPurchaseTotal() :
                (qty != null && price != null ? qty.multiply(price) : e.getTotalAmount());

        dto.setQuantity(qty);
        dto.setUnitPrice(price);
        dto.setTotalAmount(total);
        dto.setPurchaseDate(e.getPurchaseDate());
        dto.setStatus(e.getStatus());
        dto.setApprovedBy(e.getApprovedBy());
        dto.setNotes(e.getNotes());

        ingredientMasterRepository.findByIngredientIdAndStoreId(e.getIngredientId(), e.getStoreId())
                .ifPresent(i -> dto.setIngredientName(i.getIngredientName()));
        if (e.getSupplierId() != null) {
            supplierMasterRepository.findBySupplierIdAndStoreId(e.getSupplierId(), e.getStoreId())
                    .ifPresent(s -> dto.setSupplierName(s.getSupplierName()));
        }
        return dto;
    }
}
