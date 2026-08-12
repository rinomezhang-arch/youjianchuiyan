package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "banquet_notice", indexes = {
        @Index(name = "idx_banquet_notice_store_date", columnList = "store_id,banquet_date"),
        @Index(name = "idx_banquet_notice_store_status", columnList = "store_id,status")
})
public class BanquetNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_no", nullable = false, unique = true, length = 40)
    private String noticeNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "booking_id", length = 40)
    private String bookingId;

    @Column(name = "banquet_date", nullable = false)
    private LocalDate banquetDate;

    @Column(name = "banquet_time", length = 20)
    private String banquetTime;

    @Column(name = "location", nullable = false, length = 120)
    private String location;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "banquet_type", nullable = false, length = 60)
    private String banquetType;

    @Column(name = "customer_name", length = 80)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Lob
    @Column(name = "menu_content", columnDefinition = "LONGTEXT")
    private String menuContent;

    @Lob
    @Column(name = "department_items", nullable = false, columnDefinition = "LONGTEXT")
    private String departmentItems;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "scan_url", length = 500)
    private String scanUrl;

    @Column(name = "scan_name", length = 255)
    private String scanName;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "draft";
        if (departmentItems == null) departmentItems = "[]";
        if (version == null) version = 0L;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
