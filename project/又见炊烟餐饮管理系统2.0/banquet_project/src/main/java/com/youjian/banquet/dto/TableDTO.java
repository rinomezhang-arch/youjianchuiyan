/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.TableDTO
 */
package com.youjian.banquet.dto;

public class TableDTO {
    private String tableId;
    private String storeId;
    private String tableName;
    private Integer capacity;
    private String area;
    private String floor;
    private String status;
    private Integer sortOrder;
    private String notes;

    public String getTableId() {
        return this.tableId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getTableName() {
        return this.tableName;
    }

    public Integer getCapacity() {
        return this.capacity;
    }

    public String getArea() {
        return this.area;
    }

    public String getFloor() {
        return this.floor;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TableDTO)) {
            return false;
        }
        TableDTO other = (TableDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$capacity = this.getCapacity();
        Integer other$capacity = other.getCapacity();
        if (this$capacity == null ? other$capacity != null : !((Object)this$capacity).equals(other$capacity)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        String this$tableId = this.getTableId();
        String other$tableId = other.getTableId();
        if (this$tableId == null ? other$tableId != null : !this$tableId.equals(other$tableId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$tableName = this.getTableName();
        String other$tableName = other.getTableName();
        if (this$tableName == null ? other$tableName != null : !this$tableName.equals(other$tableName)) {
            return false;
        }
        String this$area = this.getArea();
        String other$area = other.getArea();
        if (this$area == null ? other$area != null : !this$area.equals(other$area)) {
            return false;
        }
        String this$floor = this.getFloor();
        String other$floor = other.getFloor();
        if (this$floor == null ? other$floor != null : !this$floor.equals(other$floor)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$notes = this.getNotes();
        String other$notes = other.getNotes();
        return !(this$notes == null ? other$notes != null : !this$notes.equals(other$notes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TableDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $capacity = this.getCapacity();
        result = result * 59 + ($capacity == null ? 43 : ((Object)$capacity).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        String $tableId = this.getTableId();
        result = result * 59 + ($tableId == null ? 43 : $tableId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $tableName = this.getTableName();
        result = result * 59 + ($tableName == null ? 43 : $tableName.hashCode());
        String $area = this.getArea();
        result = result * 59 + ($area == null ? 43 : $area.hashCode());
        String $floor = this.getFloor();
        result = result * 59 + ($floor == null ? 43 : $floor.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $notes = this.getNotes();
        result = result * 59 + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    public String toString() {
        return "TableDTO(tableId=" + this.getTableId() + ", storeId=" + this.getStoreId() + ", tableName=" + this.getTableName() + ", capacity=" + this.getCapacity() + ", area=" + this.getArea() + ", floor=" + this.getFloor() + ", status=" + this.getStatus() + ", sortOrder=" + this.getSortOrder() + ", notes=" + this.getNotes() + ")";
    }

    public TableDTO() {
    }

    public TableDTO(String tableId, String storeId, String tableName, Integer capacity, String area, String floor, String status, Integer sortOrder, String notes) {
        this.tableId = tableId;
        this.storeId = storeId;
        this.tableName = tableName;
        this.capacity = capacity;
        this.area = area;
        this.floor = floor;
        this.status = status;
        this.sortOrder = sortOrder;
        this.notes = notes;
    }
}

