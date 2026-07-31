/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.CustomerMaster
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="customer_master")
public class CustomerMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="customer_id")
    private Integer customerId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="customer_name")
    private String customerName;
    @Column(name="customer_phone")
    private String customerPhone;
    @Column(name="customer_preference", columnDefinition="TEXT")
    private String customerPreference;
    @Column(name="total_amount", precision=12, scale=2)
    private BigDecimal totalAmount;
    @Column(name="member_level")
    private String memberLevel;
    @Column(name="booking_count")
    private Integer bookingCount;
    @Column(name="last_booking_date")
    private LocalDate lastBookingDate;
    @Column(name="remark", columnDefinition="TEXT")
    private String remark;
    @Column(name="is_active")
    private Integer isActive;
    @Column(name="create_time")
    private LocalDateTime createdAt;
    @Column(name="update_time")
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

    public Integer getCustomerId() {
        return this.customerId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public String getCustomerPhone() {
        return this.customerPhone;
    }

    public String getCustomerPreference() {
        return this.customerPreference;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getMemberLevel() {
        return this.memberLevel;
    }

    public Integer getBookingCount() {
        return this.bookingCount;
    }

    public LocalDate getLastBookingDate() {
        return this.lastBookingDate;
    }

    public String getRemark() {
        return this.remark;
    }

    public Integer getIsActive() {
        return this.isActive;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public void setCustomerPreference(String customerPreference) {
        this.customerPreference = customerPreference;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setMemberLevel(String memberLevel) {
        this.memberLevel = memberLevel;
    }

    public void setBookingCount(Integer bookingCount) {
        this.bookingCount = bookingCount;
    }

    public void setLastBookingDate(LocalDate lastBookingDate) {
        this.lastBookingDate = lastBookingDate;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
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
        if (!(o instanceof CustomerMaster)) {
            return false;
        }
        CustomerMaster other = (CustomerMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$customerId = this.getCustomerId();
        Integer other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !((Object)this$customerId).equals(other$customerId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$bookingCount = this.getBookingCount();
        Integer other$bookingCount = other.getBookingCount();
        if (this$bookingCount == null ? other$bookingCount != null : !((Object)this$bookingCount).equals(other$bookingCount)) {
            return false;
        }
        Integer this$isActive = this.getIsActive();
        Integer other$isActive = other.getIsActive();
        if (this$isActive == null ? other$isActive != null : !((Object)this$isActive).equals(other$isActive)) {
            return false;
        }
        String this$customerName = this.getCustomerName();
        String other$customerName = other.getCustomerName();
        if (this$customerName == null ? other$customerName != null : !this$customerName.equals(other$customerName)) {
            return false;
        }
        String this$customerPhone = this.getCustomerPhone();
        String other$customerPhone = other.getCustomerPhone();
        if (this$customerPhone == null ? other$customerPhone != null : !this$customerPhone.equals(other$customerPhone)) {
            return false;
        }
        String this$customerPreference = this.getCustomerPreference();
        String other$customerPreference = other.getCustomerPreference();
        if (this$customerPreference == null ? other$customerPreference != null : !this$customerPreference.equals(other$customerPreference)) {
            return false;
        }
        BigDecimal this$totalAmount = this.getTotalAmount();
        BigDecimal other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !((Object)this$totalAmount).equals(other$totalAmount)) {
            return false;
        }
        String this$memberLevel = this.getMemberLevel();
        String other$memberLevel = other.getMemberLevel();
        if (this$memberLevel == null ? other$memberLevel != null : !this$memberLevel.equals(other$memberLevel)) {
            return false;
        }
        LocalDate this$lastBookingDate = this.getLastBookingDate();
        LocalDate other$lastBookingDate = other.getLastBookingDate();
        if (this$lastBookingDate == null ? other$lastBookingDate != null : !((Object)this$lastBookingDate).equals(other$lastBookingDate)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
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
        return other instanceof CustomerMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : ((Object)$customerId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $bookingCount = this.getBookingCount();
        result = result * 59 + ($bookingCount == null ? 43 : ((Object)$bookingCount).hashCode());
        Integer $isActive = this.getIsActive();
        result = result * 59 + ($isActive == null ? 43 : ((Object)$isActive).hashCode());
        String $customerName = this.getCustomerName();
        result = result * 59 + ($customerName == null ? 43 : $customerName.hashCode());
        String $customerPhone = this.getCustomerPhone();
        result = result * 59 + ($customerPhone == null ? 43 : $customerPhone.hashCode());
        String $customerPreference = this.getCustomerPreference();
        result = result * 59 + ($customerPreference == null ? 43 : $customerPreference.hashCode());
        BigDecimal $totalAmount = this.getTotalAmount();
        result = result * 59 + ($totalAmount == null ? 43 : ((Object)$totalAmount).hashCode());
        String $memberLevel = this.getMemberLevel();
        result = result * 59 + ($memberLevel == null ? 43 : $memberLevel.hashCode());
        LocalDate $lastBookingDate = this.getLastBookingDate();
        result = result * 59 + ($lastBookingDate == null ? 43 : ((Object)$lastBookingDate).hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "CustomerMaster(customerId=" + this.getCustomerId() + ", storeId=" + this.getStoreId() + ", customerName=" + this.getCustomerName() + ", customerPhone=" + this.getCustomerPhone() + ", customerPreference=" + this.getCustomerPreference() + ", totalAmount=" + String.valueOf(this.getTotalAmount()) + ", memberLevel=" + this.getMemberLevel() + ", bookingCount=" + this.getBookingCount() + ", lastBookingDate=" + String.valueOf(this.getLastBookingDate()) + ", remark=" + this.getRemark() + ", isActive=" + this.getIsActive() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public CustomerMaster() {
    }

    public CustomerMaster(Integer customerId, Long storeId, String customerName, String customerPhone, String customerPreference, BigDecimal totalAmount, String memberLevel, Integer bookingCount, LocalDate lastBookingDate, String remark, Integer isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.customerId = customerId;
        this.storeId = storeId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerPreference = customerPreference;
        this.totalAmount = totalAmount;
        this.memberLevel = memberLevel;
        this.bookingCount = bookingCount;
        this.lastBookingDate = lastBookingDate;
        this.remark = remark;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

