/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.PackageDishDetail
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
import java.time.LocalDateTime;

@Entity
@Table(name="package_dish_detail")
public class PackageDishDetail {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="detail_id")
    private Long detailId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="package_id")
    private String packageId;
    @Column(name="dish_id")
    private String dishId;
    @Column(name="dish_quantity")
    private Integer dishQuantity;
    @Column(name="dish_order")
    private Integer dishOrder;
    @Column(name="custom_name")
    private String customName;
    @Column(name="note")
    private String note;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getDetailId() {
        return this.detailId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getPackageId() {
        return this.packageId;
    }

    public String getDishId() {
        return this.dishId;
    }

    public Integer getDishQuantity() {
        return this.dishQuantity;
    }

    public Integer getDishOrder() {
        return this.dishOrder;
    }

    public String getCustomName() {
        return this.customName;
    }

    public String getNote() {
        return this.note;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setDishQuantity(Integer dishQuantity) {
        this.dishQuantity = dishQuantity;
    }

    public void setDishOrder(Integer dishOrder) {
        this.dishOrder = dishOrder;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PackageDishDetail)) {
            return false;
        }
        PackageDishDetail other = (PackageDishDetail)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$detailId = this.getDetailId();
        Long other$detailId = other.getDetailId();
        if (this$detailId == null ? other$detailId != null : !((Object)this$detailId).equals(other$detailId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$dishQuantity = this.getDishQuantity();
        Integer other$dishQuantity = other.getDishQuantity();
        if (this$dishQuantity == null ? other$dishQuantity != null : !((Object)this$dishQuantity).equals(other$dishQuantity)) {
            return false;
        }
        Integer this$dishOrder = this.getDishOrder();
        Integer other$dishOrder = other.getDishOrder();
        if (this$dishOrder == null ? other$dishOrder != null : !((Object)this$dishOrder).equals(other$dishOrder)) {
            return false;
        }
        String this$packageId = this.getPackageId();
        String other$packageId = other.getPackageId();
        if (this$packageId == null ? other$packageId != null : !this$packageId.equals(other$packageId)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$customName = this.getCustomName();
        String other$customName = other.getCustomName();
        if (this$customName == null ? other$customName != null : !this$customName.equals(other$customName)) {
            return false;
        }
        String this$note = this.getNote();
        String other$note = other.getNote();
        if (this$note == null ? other$note != null : !this$note.equals(other$note)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PackageDishDetail;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $detailId = this.getDetailId();
        result = result * 59 + ($detailId == null ? 43 : ((Object)$detailId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $dishQuantity = this.getDishQuantity();
        result = result * 59 + ($dishQuantity == null ? 43 : ((Object)$dishQuantity).hashCode());
        Integer $dishOrder = this.getDishOrder();
        result = result * 59 + ($dishOrder == null ? 43 : ((Object)$dishOrder).hashCode());
        String $packageId = this.getPackageId();
        result = result * 59 + ($packageId == null ? 43 : $packageId.hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $customName = this.getCustomName();
        result = result * 59 + ($customName == null ? 43 : $customName.hashCode());
        String $note = this.getNote();
        result = result * 59 + ($note == null ? 43 : $note.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "PackageDishDetail(detailId=" + this.getDetailId() + ", storeId=" + this.getStoreId() + ", packageId=" + this.getPackageId() + ", dishId=" + this.getDishId() + ", dishQuantity=" + this.getDishQuantity() + ", dishOrder=" + this.getDishOrder() + ", customName=" + this.getCustomName() + ", note=" + this.getNote() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public PackageDishDetail() {
    }

    public PackageDishDetail(Long detailId, Long storeId, String packageId, String dishId, Integer dishQuantity, Integer dishOrder, String customName, String note, LocalDateTime createdAt) {
        this.detailId = detailId;
        this.storeId = storeId;
        this.packageId = packageId;
        this.dishId = dishId;
        this.dishQuantity = dishQuantity;
        this.dishOrder = dishOrder;
        this.customName = customName;
        this.note = note;
        this.createdAt = createdAt;
    }
}

