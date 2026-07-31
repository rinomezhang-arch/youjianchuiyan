/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.KitchenLog
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="kitchen_log")
public class KitchenLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="action")
    private String action;
    @Column(name="target_type")
    private String targetType;
    @Column(name="booking_id")
    private String bookingId;
    @Column(name="dish_id")
    private String dishId;
    @Column(name="dish_name")
    private String dishName;
    @Column(name="operator_id")
    private Integer operatorId;
    @Column(name="operator_name")
    private String operatorName;
    @Column(name="note", columnDefinition="TEXT")
    private String note;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getAction() {
        return this.action;
    }

    public String getTargetType() {
        return this.targetType;
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

    public Integer getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public String getNote() {
        return this.note;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
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

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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
        if (!(o instanceof KitchenLog)) {
            return false;
        }
        KitchenLog other = (KitchenLog)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$operatorId = this.getOperatorId();
        Integer other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !((Object)this$operatorId).equals(other$operatorId)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
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
        String this$operatorName = this.getOperatorName();
        String other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) {
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
        return other instanceof KitchenLog;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $operatorId = this.getOperatorId();
        result = result * 59 + ($operatorId == null ? 43 : ((Object)$operatorId).hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $bookingId = this.getBookingId();
        result = result * 59 + ($bookingId == null ? 43 : $bookingId.hashCode());
        String $dishId = this.getDishId();
        result = result * 59 + ($dishId == null ? 43 : $dishId.hashCode());
        String $dishName = this.getDishName();
        result = result * 59 + ($dishName == null ? 43 : $dishName.hashCode());
        String $operatorName = this.getOperatorName();
        result = result * 59 + ($operatorName == null ? 43 : $operatorName.hashCode());
        String $note = this.getNote();
        result = result * 59 + ($note == null ? 43 : $note.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "KitchenLog(id=" + this.getId() + ", storeId=" + this.getStoreId() + ", action=" + this.getAction() + ", targetType=" + this.getTargetType() + ", bookingId=" + this.getBookingId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", note=" + this.getNote() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public KitchenLog() {
    }

    public KitchenLog(Long id, Long storeId, String action, String targetType, String bookingId, String dishId, String dishName, Integer operatorId, String operatorName, String note, LocalDateTime createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.action = action;
        this.targetType = targetType;
        this.bookingId = bookingId;
        this.dishId = dishId;
        this.dishName = dishName;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.note = note;
        this.createdAt = createdAt;
    }
}

