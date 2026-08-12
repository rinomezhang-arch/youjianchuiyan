/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.PurchaseDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchaseDTO {
    private Long purchaseId;
    private String storeId;
    private String ingredientId;
    private String ingredientName;
    private String supplierId;
    private String supplierName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDate purchaseDate;
    private String status;
    private String approvedBy;
    private String notes;

    public Long getPurchaseId() {
        return this.purchaseId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getIngredientName() {
        return this.ingredientName;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public LocalDate getPurchaseDate() {
        return this.purchaseDate;
    }

    public String getStatus() {
        return this.status;
    }

    public String getApprovedBy() {
        return this.approvedBy;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PurchaseDTO)) {
            return false;
        }
        PurchaseDTO other = (PurchaseDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$purchaseId = this.getPurchaseId();
        Long other$purchaseId = other.getPurchaseId();
        if (this$purchaseId == null ? other$purchaseId != null : !((Object)this$purchaseId).equals(other$purchaseId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
            return false;
        }
        String this$ingredientName = this.getIngredientName();
        String other$ingredientName = other.getIngredientName();
        if (this$ingredientName == null ? other$ingredientName != null : !this$ingredientName.equals(other$ingredientName)) {
            return false;
        }
        String this$supplierId = this.getSupplierId();
        String other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) {
            return false;
        }
        String this$supplierName = this.getSupplierName();
        String other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) {
            return false;
        }
        BigDecimal this$quantity = this.getQuantity();
        BigDecimal other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity)) {
            return false;
        }
        BigDecimal this$unitPrice = this.getUnitPrice();
        BigDecimal other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !((Object)this$unitPrice).equals(other$unitPrice)) {
            return false;
        }
        BigDecimal this$totalAmount = this.getTotalAmount();
        BigDecimal other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !((Object)this$totalAmount).equals(other$totalAmount)) {
            return false;
        }
        LocalDate this$purchaseDate = this.getPurchaseDate();
        LocalDate other$purchaseDate = other.getPurchaseDate();
        if (this$purchaseDate == null ? other$purchaseDate != null : !((Object)this$purchaseDate).equals(other$purchaseDate)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$approvedBy = this.getApprovedBy();
        String other$approvedBy = other.getApprovedBy();
        if (this$approvedBy == null ? other$approvedBy != null : !this$approvedBy.equals(other$approvedBy)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PurchaseDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $purchaseId = this.getPurchaseId();
        result = result * 59 + ($purchaseId == null ? 43 : ((Object)$purchaseId).hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        String $ingredientName = this.getIngredientName();
        result = result * 59 + ($ingredientName == null ? 43 : $ingredientName.hashCode());
        String $supplierId = this.getSupplierId();
        result = result * 59 + ($supplierId == null ? 43 : $supplierId.hashCode());
        String $supplierName = this.getSupplierName();
        result = result * 59 + ($supplierName == null ? 43 : $supplierName.hashCode());
        BigDecimal $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        BigDecimal $unitPrice = this.getUnitPrice();
        result = result * 59 + ($unitPrice == null ? 43 : ((Object)$unitPrice).hashCode());
        BigDecimal $totalAmount = this.getTotalAmount();
        result = result * 59 + ($totalAmount == null ? 43 : ((Object)$totalAmount).hashCode());
        LocalDate $purchaseDate = this.getPurchaseDate();
        result = result * 59 + ($purchaseDate == null ? 43 : ((Object)$purchaseDate).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $approvedBy = this.getApprovedBy();
        result = result * 59 + ($approvedBy == null ? 43 : $approvedBy.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    public String toString() {
        return "PurchaseDTO(purchaseId=" + this.getPurchaseId() + ", storeId=" + this.getStoreId() + ", ingredientId=" + this.getIngredientId() + ", ingredientName=" + this.getIngredientName() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", quantity=" + String.valueOf(this.getQuantity()) + ", unitPrice=" + String.valueOf(this.getUnitPrice()) + ", totalAmount=" + String.valueOf(this.getTotalAmount()) + ", purchaseDate=" + String.valueOf(this.getPurchaseDate()) + ", status=" + this.getStatus() + ", approvedBy=" + this.getApprovedBy() + ", notes=" + this.getNotes() + ")";
    }

    public PurchaseDTO() {
    }

    public PurchaseDTO(Long purchaseId, String storeId, String ingredientId, String ingredientName, String supplierId, String supplierName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalAmount, LocalDate purchaseDate, String status, String approvedBy, String notes) {
        this.purchaseId = purchaseId;
        this.storeId = storeId;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.purchaseDate = purchaseDate;
        this.status = status;
        this.approvedBy = approvedBy;
        this.notes = notes;
    }
}

