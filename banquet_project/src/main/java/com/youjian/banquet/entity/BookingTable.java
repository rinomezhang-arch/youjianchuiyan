/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.BookingTable
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Index
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name="booking_table", indexes={@Index(name="idx_booking_master", columnList="booking_master_id"), @Index(name="idx_booking_id", columnList="booking_id"), @Index(name="idx_table_id", columnList="table_id")})
public class BookingTable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="table_booking_id")
    private Long tableBookingId;
    @Column(name="booking_master_id")
    private Long bookingMasterId;
    @Column(name="booking_table_code", length=50)
    private String bookingTableCode;
    @Column(name="store_id", nullable=false)
    private Long storeId;
    @Column(name="booking_id", length=20)
    private String bookingId;
    @Column(name="booking_date")
    private LocalDate bookingDate;
    @Column(name="booking_time")
    private LocalTime bookingTime;
    @Column(name="table_id")
    private Integer tableId;
    @Column(name="table_number", length=10)
    private String tableNumber;
    @Column(name="table_name", length=20)
    private String tableName;
    @Column(name="guest_count")
    private Integer guestCount;
    @Column(name="package_id", length=20)
    private String packageId;
    @Column(name="package_name", length=100)
    private String packageName;
    @Column(name="open_table_type", length=50)
    private String openTableType;
    @Column(name="table_note", length=255)
    private String tableNote;
    @Column(name="created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getTableBookingId() {
        return this.tableBookingId;
    }

    public Long getBookingMasterId() {
        return this.bookingMasterId;
    }

    public String getBookingTableCode() {
        return this.bookingTableCode;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getBookingId() {
        return this.bookingId;
    }

    public LocalDate getBookingDate() {
        return this.bookingDate;
    }

    public LocalTime getBookingTime() {
        return this.bookingTime;
    }

    public Integer getTableId() {
        return this.tableId;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getTableName() {
        return this.tableName;
    }

    public Integer getGuestCount() {
        return this.guestCount;
    }

    public String getPackageId() {
        return this.packageId;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getOpenTableType() {
        return this.openTableType;
    }

    public String getTableNote() {
        return this.tableNote;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setTableBookingId(Long tableBookingId) {
        this.tableBookingId = tableBookingId;
    }

    public void setBookingMasterId(Long bookingMasterId) {
        this.bookingMasterId = bookingMasterId;
    }

    public void setBookingTableCode(String bookingTableCode) {
        this.bookingTableCode = bookingTableCode;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public void setBookingTime(LocalTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setOpenTableType(String openTableType) {
        this.openTableType = openTableType;
    }

    public void setTableNote(String tableNote) {
        this.tableNote = tableNote;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BookingTable)) {
            return false;
        }
        BookingTable other = (BookingTable)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$tableBookingId = this.getTableBookingId();
        Long other$tableBookingId = other.getTableBookingId();
        if (this$tableBookingId == null ? other$tableBookingId != null : !((Object)this$tableBookingId).equals(other$tableBookingId)) {
            return false;
        }
        Long this$bookingMasterId = this.getBookingMasterId();
        Long other$bookingMasterId = other.getBookingMasterId();
        if (this$bookingMasterId == null ? other$bookingMasterId != null : !((Object)this$bookingMasterId).equals(other$bookingMasterId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$tableId = this.getTableId();
        Integer other$tableId = other.getTableId();
        if (this$tableId == null ? other$tableId != null : !((Object)this$tableId).equals(other$tableId)) {
            return false;
        }
        Integer this$guestCount = this.getGuestCount();
        Integer other$guestCount = other.getGuestCount();
        if (this$guestCount == null ? other$guestCount != null : !((Object)this$guestCount).equals(other$guestCount)) {
            return false;
        }
        String this$bookingTableCode = this.getBookingTableCode();
        String other$bookingTableCode = other.getBookingTableCode();
        if (this$bookingTableCode == null ? other$bookingTableCode != null : !this$bookingTableCode.equals(other$bookingTableCode)) {
            return false;
        }
        String this$bookingId = this.getBookingId();
        String other$bookingId = other.getBookingId();
        if (this$bookingId == null ? other$bookingId != null : !this$bookingId.equals(other$bookingId)) {
            return false;
        }
        LocalDate this$bookingDate = this.getBookingDate();
        LocalDate other$bookingDate = other.getBookingDate();
        if (this$bookingDate == null ? other$bookingDate != null : !((Object)this$bookingDate).equals(other$bookingDate)) {
            return false;
        }
        LocalTime this$bookingTime = this.getBookingTime();
        LocalTime other$bookingTime = other.getBookingTime();
        if (this$bookingTime == null ? other$bookingTime != null : !((Object)this$bookingTime).equals(other$bookingTime)) {
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
        String this$packageId = this.getPackageId();
        String other$packageId = other.getPackageId();
        if (this$packageId == null ? other$packageId != null : !this$packageId.equals(other$packageId)) {
            return false;
        }
        String this$packageName = this.getPackageName();
        String other$packageName = other.getPackageName();
        if (this$packageName == null ? other$packageName != null : !this$packageName.equals(other$packageName)) {
            return false;
        }
        String this$openTableType = this.getOpenTableType();
        String other$openTableType = other.getOpenTableType();
        if (this$openTableType == null ? other$openTableType != null : !this$openTableType.equals(other$openTableType)) {
            return false;
        }
        String this$tableNote = this.getTableNote();
        String other$tableNote = other.getTableNote();
        if (this$tableNote == null ? other$tableNote != null : !this$tableNote.equals(other$tableNote)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof BookingTable;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $tableBookingId = this.getTableBookingId();
        result = result * 59 + ($tableBookingId == null ? 43 : ((Object)$tableBookingId).hashCode());
        Long $bookingMasterId = this.getBookingMasterId();
        result = result * 59 + ($bookingMasterId == null ? 43 : ((Object)$bookingMasterId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $tableId = this.getTableId();
        result = result * 59 + ($tableId == null ? 43 : ((Object)$tableId).hashCode());
        Integer $guestCount = this.getGuestCount();
        result = result * 59 + ($guestCount == null ? 43 : ((Object)$guestCount).hashCode());
        String $bookingTableCode = this.getBookingTableCode();
        result = result * 59 + ($bookingTableCode == null ? 43 : $bookingTableCode.hashCode());
        String $bookingId = this.getBookingId();
        result = result * 59 + ($bookingId == null ? 43 : $bookingId.hashCode());
        LocalDate $bookingDate = this.getBookingDate();
        result = result * 59 + ($bookingDate == null ? 43 : ((Object)$bookingDate).hashCode());
        LocalTime $bookingTime = this.getBookingTime();
        result = result * 59 + ($bookingTime == null ? 43 : ((Object)$bookingTime).hashCode());
        String $tableNumber = this.getTableNumber();
        result = result * 59 + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        String $tableName = this.getTableName();
        result = result * 59 + ($tableName == null ? 43 : $tableName.hashCode());
        String $packageId = this.getPackageId();
        result = result * 59 + ($packageId == null ? 43 : $packageId.hashCode());
        String $packageName = this.getPackageName();
        result = result * 59 + ($packageName == null ? 43 : $packageName.hashCode());
        String $openTableType = this.getOpenTableType();
        result = result * 59 + ($openTableType == null ? 43 : $openTableType.hashCode());
        String $tableNote = this.getTableNote();
        result = result * 59 + ($tableNote == null ? 43 : $tableNote.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "BookingTable(tableBookingId=" + this.getTableBookingId() + ", bookingMasterId=" + this.getBookingMasterId() + ", bookingTableCode=" + this.getBookingTableCode() + ", storeId=" + this.getStoreId() + ", bookingId=" + this.getBookingId() + ", bookingDate=" + String.valueOf(this.getBookingDate()) + ", bookingTime=" + String.valueOf(this.getBookingTime()) + ", tableId=" + this.getTableId() + ", tableNumber=" + this.getTableNumber() + ", tableName=" + this.getTableName() + ", guestCount=" + this.getGuestCount() + ", packageId=" + this.getPackageId() + ", packageName=" + this.getPackageName() + ", openTableType=" + this.getOpenTableType() + ", tableNote=" + this.getTableNote() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public BookingTable() {
    }

    public BookingTable(Long tableBookingId, Long bookingMasterId, String bookingTableCode, Long storeId, String bookingId, LocalDate bookingDate, LocalTime bookingTime, Integer tableId, String tableNumber, String tableName, Integer guestCount, String packageId, String packageName, String openTableType, String tableNote, LocalDateTime createdAt) {
        this.tableBookingId = tableBookingId;
        this.bookingMasterId = bookingMasterId;
        this.bookingTableCode = bookingTableCode;
        this.storeId = storeId;
        this.bookingId = bookingId;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.tableName = tableName;
        this.guestCount = guestCount;
        this.packageId = packageId;
        this.packageName = packageName;
        this.openTableType = openTableType;
        this.tableNote = tableNote;
        this.createdAt = createdAt;
    }
}

