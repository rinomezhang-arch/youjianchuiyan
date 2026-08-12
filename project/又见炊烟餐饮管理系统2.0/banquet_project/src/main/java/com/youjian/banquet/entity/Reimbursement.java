package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reimbursement")
public class Reimbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reimbursement_id")
    private Long reimbursementId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "reimbursement_no", nullable = false, unique = true, length = 50)
    private String reimbursementNo;

    @Column(name = "applicant_id")
    private Integer applicantId;

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "department_name", length = 50)
    private String departmentName;

    @Column(name = "reimburse_date", nullable = false)
    private LocalDate reimburseDate;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reimburse_type", length = 50)
    private String reimburseType;

    @Column(name = "receipt_count")
    private Integer receiptCount;

    @Column(name = "receipt_file_path", length = 255)
    private String receiptFilePath;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_name", length = 50)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "approve_comment", columnDefinition = "TEXT")
    private String approveComment;

    @Column(name = "finance_approver_id")
    private Integer financeApproverId;

    @Column(name = "finance_approver_name", length = 50)
    private String financeApproverName;

    @Column(name = "finance_approve_time")
    private LocalDateTime financeApproveTime;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
