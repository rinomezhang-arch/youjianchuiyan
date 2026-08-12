/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.InventoryDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class InventoryDTO {
    private String ingredientId;
    private String storeId;
    private String ingredientName;
    private String changeType;
    private BigDecimal quantity;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
    private String referenceId;
    private String referenceType;
    private String operator;
    private String notes;

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getIngredientName() {
        return this.ingredientName;
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

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InventoryDTO)) {
            return false;
        }
        InventoryDTO other = (InventoryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$ingredientName = this.getIngredientName();
        String other$ingredientName = other.getIngredientName();
        if (this$ingredientName == null ? other$ingredientName != null : !this$ingredientName.equals(other$ingredientName)) {
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
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof InventoryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $ingredientName = this.getIngredientName();
        result = result * 59 + ($ingredientName == null ? 43 : $ingredientName.hashCode());
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
        return result;
    }

    public String toString() {
        return "InventoryDTO(ingredientId=" + this.getIngredientId() + ", storeId=" + this.getStoreId() + ", ingredientName=" + this.getIngredientName() + ", changeType=" + this.getChangeType() + ", quantity=" + String.valueOf(this.getQuantity()) + ", beforeStock=" + String.valueOf(this.getBeforeStock()) + ", afterStock=" + String.valueOf(this.getAfterStock()) + ", referenceId=" + this.getReferenceId() + ", referenceType=" + this.getReferenceType() + ", operator=" + this.getOperator() + ", notes=" + this.getNotes() + ")";
    }

    public InventoryDTO() {
    }

    public InventoryDTO(String ingredientId, String storeId, String ingredientName, String changeType, BigDecimal quantity, BigDecimal beforeStock, BigDecimal afterStock, String referenceId, String referenceType, String operator, String notes) {
        this.ingredientId = ingredientId;
        this.storeId = storeId;
        this.ingredientName = ingredientName;
        this.changeType = changeType;
        this.quantity = quantity;
        this.beforeStock = beforeStock;
        this.afterStock = afterStock;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.operator = operator;
        this.notes = notes;
    }
}

