/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.TableMaster
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
import java.time.LocalDateTime;

@Entity
@Table(name="table_master")
public class TableMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="table_id")
    private Integer tableId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="table_number")
    private String tableNumber;
    @Column(name="table_name")
    private String tableName;
    @Column(name="table_location")
    private String tableLocation;
    @Column(name="table_area")
    private String tableArea;
    @Column(name="table_capacity")
    private Integer tableCapacity;
    @Column(name="table_type")
    private String tableType;
    @Column(name="table_status")
    private String tableStatus;
    @Column(name="min_capacity")
    private Integer minCapacity;
    @Column(name="max_capacity")
    private Integer maxCapacity;
    @Column(name="sort_order")
    private Integer sortOrder;
    @Column(name="is_active")
    private Integer isActive;
    @Column(name="remark", columnDefinition="TEXT")
    private String remark;
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

    public Integer getTableId() {
        return this.tableId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getTableLocation() {
        return this.tableLocation;
    }

    public String getTableArea() {
        return this.tableArea;
    }

    public Integer getTableCapacity() {
        return this.tableCapacity;
    }

    public String getTableType() {
        return this.tableType;
    }

    public String getTableStatus() {
        return this.tableStatus;
    }

    public Integer getMinCapacity() {
        return this.minCapacity;
    }

    public Integer getMaxCapacity() {
        return this.maxCapacity;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Integer getIsActive() {
        return this.isActive;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setTableLocation(String tableLocation) {
        this.tableLocation = tableLocation;
    }

    public void setTableArea(String tableArea) {
        this.tableArea = tableArea;
    }

    public void setTableCapacity(Integer tableCapacity) {
        this.tableCapacity = tableCapacity;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }

    public void setMinCapacity(Integer minCapacity) {
        this.minCapacity = minCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
        if (!(o instanceof TableMaster)) {
            return false;
        }
        TableMaster other = (TableMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$tableId = this.getTableId();
        Integer other$tableId = other.getTableId();
        if (this$tableId == null ? other$tableId != null : !((Object)this$tableId).equals(other$tableId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$tableCapacity = this.getTableCapacity();
        Integer other$tableCapacity = other.getTableCapacity();
        if (this$tableCapacity == null ? other$tableCapacity != null : !((Object)this$tableCapacity).equals(other$tableCapacity)) {
            return false;
        }
        Integer this$minCapacity = this.getMinCapacity();
        Integer other$minCapacity = other.getMinCapacity();
        if (this$minCapacity == null ? other$minCapacity != null : !((Object)this$minCapacity).equals(other$minCapacity)) {
            return false;
        }
        Integer this$maxCapacity = this.getMaxCapacity();
        Integer other$maxCapacity = other.getMaxCapacity();
        if (this$maxCapacity == null ? other$maxCapacity != null : !((Object)this$maxCapacity).equals(other$maxCapacity)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        Integer this$isActive = this.getIsActive();
        Integer other$isActive = other.getIsActive();
        if (this$isActive == null ? other$isActive != null : !((Object)this$isActive).equals(other$isActive)) {
            return false;
        }
        String this$tableNumber = this.getTableNumber();
        String other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) {
            return false;
        }
        String this$tableName = this.getTableName();
        String other$tableName = other.getTableName();
        if (this$tableName == null ? other$tableName != null : !this$tableName.equals(other$tableName)) {
            return false;
        }
        String this$tableLocation = this.getTableLocation();
        String other$tableLocation = other.getTableLocation();
        if (this$tableLocation == null ? other$tableLocation != null : !this$tableLocation.equals(other$tableLocation)) {
            return false;
        }
        String this$tableArea = this.getTableArea();
        String other$tableArea = other.getTableArea();
        if (this$tableArea == null ? other$tableArea != null : !this$tableArea.equals(other$tableArea)) {
            return false;
        }
        String this$tableType = this.getTableType();
        String other$tableType = other.getTableType();
        if (this$tableType == null ? other$tableType != null : !this$tableType.equals(other$tableType)) {
            return false;
        }
        String this$tableStatus = this.getTableStatus();
        String other$tableStatus = other.getTableStatus();
        if (this$tableStatus == null ? other$tableStatus != null : !this$tableStatus.equals(other$tableStatus)) {
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
        return other instanceof TableMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $tableId = this.getTableId();
        result = result * 59 + ($tableId == null ? 43 : ((Object)$tableId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $tableCapacity = this.getTableCapacity();
        result = result * 59 + ($tableCapacity == null ? 43 : ((Object)$tableCapacity).hashCode());
        Integer $minCapacity = this.getMinCapacity();
        result = result * 59 + ($minCapacity == null ? 43 : ((Object)$minCapacity).hashCode());
        Integer $maxCapacity = this.getMaxCapacity();
        result = result * 59 + ($maxCapacity == null ? 43 : ((Object)$maxCapacity).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        Integer $isActive = this.getIsActive();
        result = result * 59 + ($isActive == null ? 43 : ((Object)$isActive).hashCode());
        String $tableNumber = this.getTableNumber();
        result = result * 59 + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        String $tableName = this.getTableName();
        result = result * 59 + ($tableName == null ? 43 : $tableName.hashCode());
        String $tableLocation = this.getTableLocation();
        result = result * 59 + ($tableLocation == null ? 43 : $tableLocation.hashCode());
        String $tableArea = this.getTableArea();
        result = result * 59 + ($tableArea == null ? 43 : $tableArea.hashCode());
        String $tableType = this.getTableType();
        result = result * 59 + ($tableType == null ? 43 : $tableType.hashCode());
        String $tableStatus = this.getTableStatus();
        result = result * 59 + ($tableStatus == null ? 43 : $tableStatus.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "TableMaster(tableId=" + this.getTableId() + ", storeId=" + this.getStoreId() + ", tableNumber=" + this.getTableNumber() + ", tableName=" + this.getTableName() + ", tableLocation=" + this.getTableLocation() + ", tableArea=" + this.getTableArea() + ", tableCapacity=" + this.getTableCapacity() + ", tableType=" + this.getTableType() + ", tableStatus=" + this.getTableStatus() + ", minCapacity=" + this.getMinCapacity() + ", maxCapacity=" + this.getMaxCapacity() + ", sortOrder=" + this.getSortOrder() + ", isActive=" + this.getIsActive() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public TableMaster() {
    }

    public TableMaster(Integer tableId, Long storeId, String tableNumber, String tableName, String tableLocation, String tableArea, Integer tableCapacity, String tableType, String tableStatus, Integer minCapacity, Integer maxCapacity, Integer sortOrder, Integer isActive, String remark, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.tableId = tableId;
        this.storeId = storeId;
        this.tableNumber = tableNumber;
        this.tableName = tableName;
        this.tableLocation = tableLocation;
        this.tableArea = tableArea;
        this.tableCapacity = tableCapacity;
        this.tableType = tableType;
        this.tableStatus = tableStatus;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

