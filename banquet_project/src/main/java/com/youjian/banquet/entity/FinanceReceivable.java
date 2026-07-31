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
@Table(name = "finance_receivable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceReceivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receivable_id")
    private Long receivableId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "receivable_no")
    private String receivableNo;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_no")
    private String bookingNo;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "received_amount", precision = 12, scale = 2)
    private BigDecimal receivedAmount;

    @Column(name = "pending_amount", precision = 12, scale = 2)
    private BigDecimal pendingAmount;

    @Column(name = "receivable_date")
    private LocalDate receivableDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status")
    private String status;

    @Column(name = "credit_days")
    private Integer creditDays;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "remark")
    private String remark;

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
