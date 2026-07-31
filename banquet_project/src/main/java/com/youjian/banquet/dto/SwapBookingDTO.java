/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.SwapBookingDTO
 */
package com.youjian.banquet.dto;

public class SwapBookingDTO {
    private String bookingId1;
    private String storeId;
    private String tableId1;
    private String tableId2;

    public String getBookingId1() {
        return this.bookingId1;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getTableId1() {
        return this.tableId1;
    }

    public String getTableId2() {
        return this.tableId2;
    }

    public void setBookingId1(String bookingId1) {
        this.bookingId1 = bookingId1;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setTableId1(String tableId1) {
        this.tableId1 = tableId1;
    }

    public void setTableId2(String tableId2) {
        this.tableId2 = tableId2;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SwapBookingDTO)) {
            return false;
        }
        SwapBookingDTO other = (SwapBookingDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$bookingId1 = this.getBookingId1();
        String other$bookingId1 = other.getBookingId1();
        if (this$bookingId1 == null ? other$bookingId1 != null : !this$bookingId1.equals(other$bookingId1)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$tableId1 = this.getTableId1();
        String other$tableId1 = other.getTableId1();
        if (this$tableId1 == null ? other$tableId1 != null : !this$tableId1.equals(other$tableId1)) {
            return false;
        }
        String this$tableId2 = this.getTableId2();
        String other$tableId2 = other.getTableId2();
        return !(this$tableId2 == null ? other$tableId2 != null : !this$tableId2.equals(other$tableId2));
    }

    protected boolean canEqual(Object other) {
        return other instanceof SwapBookingDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $bookingId1 = this.getBookingId1();
        result = result * 59 + ($bookingId1 == null ? 43 : $bookingId1.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $tableId1 = this.getTableId1();
        result = result * 59 + ($tableId1 == null ? 43 : $tableId1.hashCode());
        String $tableId2 = this.getTableId2();
        result = result * 59 + ($tableId2 == null ? 43 : $tableId2.hashCode());
        return result;
    }

    public String toString() {
        return "SwapBookingDTO(bookingId1=" + this.getBookingId1() + ", storeId=" + this.getStoreId() + ", tableId1=" + this.getTableId1() + ", tableId2=" + this.getTableId2() + ")";
    }

    public SwapBookingDTO() {
    }

    public SwapBookingDTO(String bookingId1, String storeId, String tableId1, String tableId2) {
        this.bookingId1 = bookingId1;
        this.storeId = storeId;
        this.tableId1 = tableId1;
        this.tableId2 = tableId2;
    }
}

