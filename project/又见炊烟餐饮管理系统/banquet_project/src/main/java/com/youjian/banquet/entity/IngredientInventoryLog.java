/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.IngredientInventoryLog
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="ingredient_inventory_log")
public class IngredientInventoryLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="log_id")
    private Long logId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="ingredient_id")
    private String ingredientId;
    @Column(name="change_type")
    private String changeType;
    @Column(name="quantity", precision=10, scale=3)
    private BigDecimal quantity;
    @Column(name="before_stock", precision=10, scale=3)
    private BigDecimal beforeStock;
    @Column(name="after_stock", precision=10, scale=3)
    private BigDecimal afterStock;
    @Column(name="reference_id")
    private String referenceId;
    @Column(name="reference_type")
    private String referenceType;
    @Column(name="operator")
    private String operator;
    @Column(name="notes", columnDefinition="TEXT")
    private String notes;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getLogId() {
        return this.logId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getChangeType() {
        return this.changeType;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public BigDecimal getBeforeStock() {
        return this.beforeStock;
    }

    public BigDecimal getAfterStock() {
        return this.afterStock;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public String getReferenceType() {
        return this.referenceType;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getNotes() {
        return this.notes;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setBeforeStock(BigDecimal beforeStock) {
        this.beforeStock = beforeStock;
    }

    public void setAfterStock(BigDecimal afterStock) {
        this.afterStock = afterStock;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IngredientInventoryLog)) {
            return false;
        }
        IngredientInventoryLog other = (IngredientInventoryLog)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$logId = this.getLogId();
        Long other$logId = other.getLogId();
        if (this$logId == null ? other$logId != null : !((Object)this$logId).equals(other$logId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
            return false;
        }
        String this$changeType = this.getChangeType();
        String other$changeType = other.getChangeType();
        if (this$changeType == null ? other$changeType != null : !this$changeType.equals(other$changeType)) {
            return false;
        }
        BigDecimal this$quantity = this.getQuantity();
        BigDecimal other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity)) {
            return false;
        }
        BigDecimal this$beforeStock = this.getBeforeStock();
        BigDecimal other$beforeStock = other.getBeforeStock();
        if (this$beforeStock == null ? other$beforeStock != null : !((Object)this$beforeStock).equals(other$beforeStock)) {
            return false;
        }
        BigDecimal this$afterStock = this.getAfterStock();
        BigDecimal other$afterStock = other.getAfterStock();
        if (this$afterStock == null ? other$afterStock != null : !((Object)this$afterStock).equals(other$afterStock)) {
            return false;
        }
        String this$referenceId = this.getReferenceId();
        String other$referenceId = other.getReferenceId();
        if (this$referenceId == null ? other$referenceId != null : !this$referenceId.equals(other$referenceId)) {
            return false;
        }
        String this$referenceType = this.getReferenceType();
        String other$referenceType = other.getReferenceType();
        if (this$referenceType == null ? other$referenceType != null : !this$referenceType.equals(other$referenceType)) {
            return false;
        }
        String this$operator = this.getOperator();
        String other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof IngredientInventoryLog;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $logId = this.getLogId();
        result = result * 59 + ($logId == null ? 43 : ((Object)$logId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        String $changeType = this.getChangeType();
        result = result * 59 + ($changeType == null ? 43 : $changeType.hashCode());
        BigDecimal $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        BigDecimal $beforeStock = this.getBeforeStock();
        result = result * 59 + ($beforeStock == null ? 43 : ((Object)$beforeStock).hashCode());
        BigDecimal $afterStock = this.getAfterStock();
        result = result * 59 + ($afterStock == null ? 43 : ((Object)$afterStock).hashCode());
        String $referenceId = this.getReferenceId();
        result = result * 59 + ($referenceId == null ? 43 : $referenceId.hashCode());
        String $referenceType = this.getReferenceType();
        result = result * 59 + ($referenceType == null ? 43 : $referenceType.hashCode());
        String $operator = this.getOperator();
        result = result * 59 + ($operator == null ? 43 : $operator.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "IngredientInventoryLog(logId=" + this.getLogId() + ", storeId=" + this.getStoreId() + ", ingredientId=" + this.getIngredientId() + ", changeType=" + this.getChangeType() + ", quantity=" + String.valueOf(this.getQuantity()) + ", beforeStock=" + String.valueOf(this.getBeforeStock()) + ", afterStock=" + String.valueOf(this.getAfterStock()) + ", referenceId=" + this.getReferenceId() + ", referenceType=" + this.getReferenceType() + ", operator=" + this.getOperator() + ", notes=" + this.getNotes() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public IngredientInventoryLog() {
    }

    public IngredientInventoryLog(Long logId, Long storeId, String ingredientId, String changeType, BigDecimal quantity, BigDecimal beforeStock, BigDecimal afterStock, String referenceId, String referenceType, String operator, String notes, LocalDateTime createdAt) {
        this.logId = logId;
        this.storeId = storeId;
        this.ingredientId = ingredientId;
        this.changeType = changeType;
        this.quantity = quantity;
        this.beforeStock = beforeStock;
        this.afterStock = afterStock;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.operator = operator;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}

