/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishRecipe
 *  com.youjian.banquet.entity.DishRecipe$DishRecipeId
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.IdClass
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="dish_recipe")
@IdClass(DishRecipe.DishRecipeId.class)
public class DishRecipe {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishRecipeId implements Serializable {
        private String dishId;
        private Long storeId;
        private String ingredientId;
    }

    @Id
    @Column(name="dish_id")
    private String dishId;
    @Id
    @Column(name="store_id")
    private Long storeId;
    @Id
    @Column(name="ingredient_id")
    private String ingredientId;
    @Column(name="quantity", precision=10, scale=3)
    private BigDecimal quantity;
    @Column(name="unit")
    private String unit;
    @Column(name="notes")
    private String notes;
    @Column(name="created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getDishId() {
        return this.dishId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getIngredientId() {
        return this.ingredientId;
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

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DishRecipe)) {
            return false;
        }
        DishRecipe other = (DishRecipe)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
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
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DishRecipe;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        BigDecimal $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        String $unit = this.getUnit();
        result = result * 59 + ($unit == null ? 43 : $unit.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "DishRecipe(dishId=" + this.getDishId() + ", storeId=" + this.getStoreId() + ", ingredientId=" + this.getIngredientId() + ", quantity=" + String.valueOf(this.getQuantity()) + ", unit=" + this.getUnit() + ", notes=" + this.getNotes() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public DishRecipe() {
    }

    public DishRecipe(String dishId, Long storeId, String ingredientId, BigDecimal quantity, String unit, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.dishId = dishId;
        this.storeId = storeId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

