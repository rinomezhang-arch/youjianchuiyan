/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BookingDishDetail
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
@Table(name="booking_dish_detail")
public class BookingDishDetail {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="dish_booking_id")
    private Long dishBookingId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="table_booking_id")
    private Long tableBookingId;
    @Column(name="booking_id")
    private String bookingId;
    @Column(name="dish_id")
    private String dishId;
    @Column(name="dish_name")
    private String dishName;
    @Column(name="dish_quantity")
    private Integer dishQuantity;
    @Column(name="unit_price", precision=10, scale=2)
    private BigDecimal unitPrice;
    @Column(name="subtotal", precision=10, scale=2)
    private BigDecimal subtotal;
    @Column(name="custom_name")
    private String customName;
    @Column(name="dish_note")
    private String dishNote;
    @Column(name="dish_order")
    private Integer dishOrder;
    @Column(name="kitchen_status")
    private String kitchenStatus;
    @Column(name="kitchen_station")
    private String kitchenStation;
    @Column(name="kitchen_note")
    private String kitchenNote;
    @Column(name="kitchen_started_at")
    private Long kitchenStartedAt;
    @Column(name="kitchen_done_at")
    private Long kitchenDoneAt;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getDishBookingId() {
        return this.dishBookingId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Long getTableBookingId() {
        return this.tableBookingId;
    }

    public String getBookingId() {
        return this.bookingId;
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public Integer getDishQuantity() {
        return this.dishQuantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getSubtotal() {
        return this.subtotal;
    }

    public String getCustomName() {
        return this.customName;
    }

    public String getDishNote() {
        return this.dishNote;
    }

    public Integer getDishOrder() {
        return this.dishOrder;
    }

    public String getKitchenStatus() {
        return this.kitchenStatus;
    }

    public String getKitchenStation() {
        return this.kitchenStation;
    }

    public String getKitchenNote() {
        return this.kitchenNote;
    }

    public Long getKitchenStartedAt() {
        return this.kitchenStartedAt;
    }

    public Long getKitchenDoneAt() {
        return this.kitchenDoneAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setDishBookingId(Long dishBookingId) {
        this.dishBookingId = dishBookingId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setTableBookingId(Long tableBookingId) {
        this.tableBookingId = tableBookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public void setDishQuantity(Integer dishQuantity) {
        this.dishQuantity = dishQuantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public void setDishNote(String dishNote) {
        this.dishNote = dishNote;
    }

    public void setDishOrder(Integer dishOrder) {
        this.dishOrder = dishOrder;
    }

    public void setKitchenStatus(String kitchenStatus) {
        this.kitchenStatus = kitchenStatus;
    }

    public void setKitchenStation(String kitchenStation) {
        this.kitchenStation = kitchenStation;
    }

    public void setKitchenNote(String kitchenNote) {
        this.kitchenNote = kitchenNote;
    }

    public void setKitchenStartedAt(Long kitchenStartedAt) {
        this.kitchenStartedAt = kitchenStartedAt;
    }

    public void setKitchenDoneAt(Long kitchenDoneAt) {
        this.kitchenDoneAt = kitchenDoneAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BookingDishDetail)) {
            return false;
        }
        BookingDishDetail other = (BookingDishDetail)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$dishBookingId = this.getDishBookingId();
        Long other$dishBookingId = other.getDishBookingId();
        if (this$dishBookingId == null ? other$dishBookingId != null : !((Object)this$dishBookingId).equals(other$dishBookingId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Long this$tableBookingId = this.getTableBookingId();
        Long other$tableBookingId = other.getTableBookingId();
        if (this$tableBookingId == null ? other$tableBookingId != null : !((Object)this$tableBookingId).equals(other$tableBookingId)) {
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
        Long this$kitchenStartedAt = this.getKitchenStartedAt();
        Long other$kitchenStartedAt = other.getKitchenStartedAt();
        if (this$kitchenStartedAt == null ? other$kitchenStartedAt != null : !((Object)this$kitchenStartedAt).equals(other$kitchenStartedAt)) {
            return false;
        }
        Long this$kitchenDoneAt = this.getKitchenDoneAt();
        Long other$kitchenDoneAt = other.getKitchenDoneAt();
        if (this$kitchenDoneAt == null ? other$kitchenDoneAt != null : !((Object)this$kitchenDoneAt).equals(other$kitchenDoneAt)) {
            return false;
        }
        String this$bookingId = this.getBookingId();
        String other$bookingId = other.getBookingId();
        if (this$bookingId == null ? other$bookingId != null : !this$bookingId.equals(other$bookingId)) {
            return false;
        }
        String this$dishId = this.getDishId();
        String other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) {
            return false;
        }
        String this$dishName = this.getDishName();
        String other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) {
            return false;
        }
        BigDecimal this$unitPrice = this.getUnitPrice();
        BigDecimal other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !((Object)this$unitPrice).equals(other$unitPrice)) {
            return false;
        }
        BigDecimal this$subtotal = this.getSubtotal();
        BigDecimal other$subtotal = other.getSubtotal();
        if (this$subtotal == null ? other$subtotal != null : !((Object)this$subtotal).equals(other$subtotal)) {
            return false;
        }
        String this$customName = this.getCustomName();
        String other$customName = other.getCustomName();
        if (this$customName == null ? other$customName != null : !this$customName.equals(other$customName)) {
            return false;
        }
        String this$dishNote = this.getDishNote();
        String other$dishNote = other.getDishNote();
        if (this$dishNote == null ? other$dishNote != null : !this$dishNote.equals(other$dishNote)) {
            return false;
        }
        String this$kitchenStatus = this.getKitchenStatus();
        String other$kitchenStatus = other.getKitchenStatus();
        if (this$kitchenStatus == null ? other$kitchenStatus != null : !this$kitchenStatus.equals(other$kitchenStatus)) {
            return false;
        }
        String this$kitchenStation = this.getKitchenStation();
        String other$kitchenStation = other.getKitchenStation();
        if (this$kitchenStation == null ? other$kitchenStation != null : !this$kitchenStation.equals(other$kitchenStation)) {
            return false;
        }
        String this$kitchenNote = this.getKitchenNote();
        String other$kitchenNote = other.getKitchenNote();
        if (this$kitchenNote == null ? other$kitchenNote != null : !this$kitchenNote.equals(other$kitchenNote)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof BookingDishDetail;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $dishBookingId = this.getDishBookingId();
        result = result * 59 + ($dishBookingId == null ? 43 : ((Object)$dishBookingId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Long $tableBookingId = this.getTableBookingId();
        result = result * 59 + ($tableBookingId == null ? 43 : ((Object)$tableBookingId).hashCode());
        Integer $dishQuantity = this.getDishQuantity();
        result = result * 59 + ($dishQuantity == null ? 43 : ((Object)$dishQuantity).hashCode());
        Integer $dishOrder = this.getDishOrder();
        result = result * 59 + ($dishOrder == null ? 43 : ((Object)$dishOrder).hashCode());
        Long $kitchenStartedAt = this.getKitchenStartedAt();
        result = result * 59 + ($kitchenStartedAt == null ? 43 : ((Object)$kitchenStartedAt).hashCode());
        Long $kitchenDoneAt = this.getKitchenDoneAt();
        result = result * 59 + ($kitchenDoneAt == null ? 43 : ((Object)$kitchenDoneAt).hashCode());
        String $bookingId = this.getBookingId();
        result = result * 59 + ($bookingId == null ? 43 : $bookingId.hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $dishName = this.getDishName();
        result = result * 59 + ($dishName == null ? 43 : $dishName.hashCode());
        BigDecimal $unitPrice = this.getUnitPrice();
        result = result * 59 + ($unitPrice == null ? 43 : ((Object)$unitPrice).hashCode());
        BigDecimal $subtotal = this.getSubtotal();
        result = result * 59 + ($subtotal == null ? 43 : ((Object)$subtotal).hashCode());
        String $customName = this.getCustomName();
        result = result * 59 + ($customName == null ? 43 : $customName.hashCode());
        String $dishNote = this.getDishNote();
        result = result * 59 + ($dishNote == null ? 43 : $dishNote.hashCode());
        String $kitchenStatus = this.getKitchenStatus();
        result = result * 59 + ($kitchenStatus == null ? 43 : $kitchenStatus.hashCode());
        String $kitchenStation = this.getKitchenStation();
        result = result * 59 + ($kitchenStation == null ? 43 : $kitchenStation.hashCode());
        String $kitchenNote = this.getKitchenNote();
        result = result * 59 + ($kitchenNote == null ? 43 : $kitchenNote.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "BookingDishDetail(dishBookingId=" + this.getDishBookingId() + ", storeId=" + this.getStoreId() + ", tableBookingId=" + this.getTableBookingId() + ", bookingId=" + this.getBookingId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", dishQuantity=" + this.getDishQuantity() + ", unitPrice=" + String.valueOf(this.getUnitPrice()) + ", subtotal=" + String.valueOf(this.getSubtotal()) + ", customName=" + this.getCustomName() + ", dishNote=" + this.getDishNote() + ", dishOrder=" + this.getDishOrder() + ", kitchenStatus=" + this.getKitchenStatus() + ", kitchenStation=" + this.getKitchenStation() + ", kitchenNote=" + this.getKitchenNote() + ", kitchenStartedAt=" + this.getKitchenStartedAt() + ", kitchenDoneAt=" + this.getKitchenDoneAt() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public BookingDishDetail() {
    }

    public BookingDishDetail(Long dishBookingId, Long storeId, Long tableBookingId, String bookingId, String dishId, String dishName, Integer dishQuantity, BigDecimal unitPrice, BigDecimal subtotal, String customName, String dishNote, Integer dishOrder, String kitchenStatus, String kitchenStation, String kitchenNote, Long kitchenStartedAt, Long kitchenDoneAt, LocalDateTime createdAt) {
        this.dishBookingId = dishBookingId;
        this.storeId = storeId;
        this.tableBookingId = tableBookingId;
        this.bookingId = bookingId;
        this.dishId = dishId;
        this.dishName = dishName;
        this.dishQuantity = dishQuantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.customName = customName;
        this.dishNote = dishNote;
        this.dishOrder = dishOrder;
        this.kitchenStatus = kitchenStatus;
        this.kitchenStation = kitchenStation;
        this.kitchenNote = kitchenNote;
        this.kitchenStartedAt = kitchenStartedAt;
        this.kitchenDoneAt = kitchenDoneAt;
        this.createdAt = createdAt;
    }
}

