/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.TableReorderDTO
 */
package com.youjian.banquet.dto;

import java.util.List;

public class TableReorderDTO {
    private String storeId;
    private List<String> tableIds;

    public String getStoreId() {
        return this.storeId;
    }

    public List<String> getTableIds() {
        return this.tableIds;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setTableIds(List<String> tableIds) {
        this.tableIds = tableIds;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TableReorderDTO)) {
            return false;
        }
        TableReorderDTO other = (TableReorderDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        List this$tableIds = this.getTableIds();
        List other$tableIds = other.getTableIds();
        return !(this$tableIds == null ? other$tableIds != null : !((Object)this$tableIds).equals(other$tableIds));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TableReorderDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        List $tableIds = this.getTableIds();
        result = result * 59 + ($tableIds == null ? 43 : ((Object)$tableIds).hashCode());
        return result;
    }

    public String toString() {
        return "TableReorderDTO(storeId=" + this.getStoreId() + ", tableIds=" + String.valueOf(this.getTableIds()) + ")";
    }

    public TableReorderDTO() {
    }

    public TableReorderDTO(String storeId, List<String> tableIds) {
        this.storeId = storeId;
        this.tableIds = tableIds;
    }
}

