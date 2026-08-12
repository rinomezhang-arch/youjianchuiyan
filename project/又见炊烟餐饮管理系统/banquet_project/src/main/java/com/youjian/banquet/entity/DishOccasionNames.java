/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.DishOccasionNames
 *  com.youjian.banquet.entity.DishOccasionNames$DishOccasionNamesId
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.IdClass
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name="dish_occasion_names")
@IdClass(DishOccasionNames.DishOccasionNamesId.class)
public class DishOccasionNames {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishOccasionNamesId implements Serializable {
        private String dishId;
        private String storeId;
        private String occasionName;
    }

    @Id
    @Column(name="dish_id")
    private String dishId;
    @Id
    @Column(name="store_id")
    private String storeId;
    @Id
    @Column(name="occasion_name")
    private String occasionName;
    @Column(name="display_name")
    private String displayName;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getOccasionName() {
        return this.occasionName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setOccasionName(String occasionName) {
        this.occasionName = occasionName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DishOccasionNames)) {
            return false;
        }
        DishOccasionNames other = (DishOccasionNames)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$occasionName = this.getOccasionName();
        String other$occasionName = other.getOccasionName();
        if (this$occasionName == null ? other$occasionName != null : !this$occasionName.equals(other$occasionName)) {
            return false;
        }
        String this$displayName = this.getDisplayName();
        String other$displayName = other.getDisplayName();
        if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DishOccasionNames;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $occasionName = this.getOccasionName();
        result = result * 59 + ($occasionName == null ? 43 : $occasionName.hashCode());
        String $displayName = this.getDisplayName();
        result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "DishOccasionNames(dishId=" + this.getDishId() + ", storeId=" + this.getStoreId() + ", occasionName=" + this.getOccasionName() + ", displayName=" + this.getDisplayName() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public DishOccasionNames() {
    }

    public DishOccasionNames(String dishId, String storeId, String occasionName, String displayName, LocalDateTime createdAt) {
        this.dishId = dishId;
        this.storeId = storeId;
        this.occasionName = occasionName;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }
}

