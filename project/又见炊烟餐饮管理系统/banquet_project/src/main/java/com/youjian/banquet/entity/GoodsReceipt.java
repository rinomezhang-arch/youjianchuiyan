package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "status")
    private String status;

    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "inspected_by")
    private String inspectedBy;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "qualified_amount", precision = 12, scale = 2)
    private BigDecimal qualifiedAmount;

    @Column(name = "unqualified_amount", precision = 12, scale = 2)
    private BigDecimal unqualifiedAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "create_time")
    private LocalDateTime createdAt;

    @Column(name = "update_time")
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
}