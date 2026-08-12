/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Department
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="department")
public class Department {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="dept_id")
    private Integer deptId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="dept_name")
    private String deptName;
    @Column(name="dept_code")
    private String deptCode;
    @Column(name="parent_id")
    private Integer parentId;
    @Column(name="sort_order")
    private Integer sortOrder;
    @Column(name="status")
    private String status;
    @Column(name="description")
    private String description;
    @Column(name="level")
    private Integer level;
    @Column(name="created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    public Integer getDeptId() {
        return this.deptId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getDeptName() {
        return this.deptName;
    }

    public String getDeptCode() {
        return this.deptCode;
    }

    public Integer getParentId() {
        return this.parentId;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getStatus() {
        return this.status;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getLevel() {
        return this.level;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Department)) {
            return false;
        }
        Department other = (Department)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$deptId = this.getDeptId();
        Integer other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !((Object)this$deptId).equals(other$deptId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$parentId = this.getParentId();
        Integer other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !((Object)this$parentId).equals(other$parentId)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        Integer this$level = this.getLevel();
        Integer other$level = other.getLevel();
        if (this$level == null ? other$level != null : !((Object)this$level).equals(other$level)) {
            return false;
        }
        String this$deptName = this.getDeptName();
        String other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName)) {
            return false;
        }
        String this$deptCode = this.getDeptCode();
        String other$deptCode = other.getDeptCode();
        if (this$deptCode == null ? other$deptCode != null : !this$deptCode.equals(other$deptCode)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
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
        return other instanceof Department;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $deptId = this.getDeptId();
        result = result * 59 + ($deptId == null ? 43 : ((Object)$deptId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $parentId = this.getParentId();
        result = result * 59 + ($parentId == null ? 43 : ((Object)$parentId).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        Integer $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : ((Object)$level).hashCode());
        String $deptName = this.getDeptName();
        result = result * 59 + ($deptName == null ? 43 : $deptName.hashCode());
        String $deptCode = this.getDeptCode();
        result = result * 59 + ($deptCode == null ? 43 : $deptCode.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "Department(deptId=" + this.getDeptId() + ", storeId=" + this.getStoreId() + ", deptName=" + this.getDeptName() + ", deptCode=" + this.getDeptCode() + ", parentId=" + this.getParentId() + ", sortOrder=" + this.getSortOrder() + ", status=" + this.getStatus() + ", description=" + this.getDescription() + ", level=" + this.getLevel() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public Department() {
    }

    public Department(Integer deptId, Long storeId, String deptName, String deptCode, Integer parentId, Integer sortOrder, String status, String description, Integer level, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.deptId = deptId;
        this.storeId = storeId;
        this.deptName = deptName;
        this.deptCode = deptCode;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.status = status;
        this.description = description;
        this.level = level;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

