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
@Table(name = "tool_issue")
public class ToolIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "issue_no", nullable = false, unique = true, length = 32)
    private String issueNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal qty;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "return_status", nullable = false, length = 16)
    private String returnStatus;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "creator", length = 64)
    private String creator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (returnStatus == null) returnStatus = "未归还";
        if (qty == null) qty = BigDecimal.ONE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
