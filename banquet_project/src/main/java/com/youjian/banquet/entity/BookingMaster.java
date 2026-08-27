package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_master")
public class BookingMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "booking_id", unique = true)
    private String bookingId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "booking_no")
    private String bookingNo;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "booking_time")
    private LocalTime bookingTime;

    @Column(name = "booking_type")
    private String bookingType;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "table_count")
    private Integer tableCount;

    @Column(name = "spare_tables")
    private Integer spareTables;

    @Column(name = "guest_per_table")
    private Integer guestPerTable;

    @Column(name = "booking_status")
    private String bookingStatus;

    @Column(name = "status")
    private String status;

    @Column(name = "banquet_name")
    private String banquetName;

    @Column(name = "occasion_type")
    private String occasionType;

    @Column(name = "package_id")
    private String packageId;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "special_request", columnDefinition = "TEXT")
    private String specialRequest;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "confirm_token")
    private String confirmToken;

    @Column(name = "guest_confirmed")
    private Integer guestConfirmed;

    @Column(name = "guest_confirm_time")
    private LocalDateTime guestConfirmTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (bookingNo == null && bookingId != null) {
            bookingNo = bookingId;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
