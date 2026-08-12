/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.RecipeItemDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

public class RecipeItemDTO {
    private String ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private String unit;
    private String notes;

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getIngredientName() {
        return this.ingredientName;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RecipeItemDTO)) {
            return false;
        }
        RecipeItemDTO other = (RecipeItemDTO)o;
        if (!other.canEqual((Object)this)) {
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
        BigDecimal this$quantity = this.getQuantity();
        BigDecimal other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity)) {
            return false;
        }
        String this$unit = this.getUnit();
        String other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof RecipeItemDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        String $ingredientName = this.getIngredientName();
        result = result * 59 + ($ingredientName == null ? 43 : $ingredientName.hashCode());
        BigDecimal $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        String $unit = this.getUnit();
        result = result * 59 + ($unit == null ? 43 : $unit.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    public String toString() {
        return "RecipeItemDTO(ingredientId=" + this.getIngredientId() + ", ingredientName=" + this.getIngredientName() + ", quantity=" + String.valueOf(this.getQuantity()) + ", unit=" + this.getUnit() + ", notes=" + this.getNotes() + ")";
    }

    public RecipeItemDTO() {
    }

    public RecipeItemDTO(String ingredientId, String ingredientName, BigDecimal quantity, String unit, String notes) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.notes = notes;
    }
}

