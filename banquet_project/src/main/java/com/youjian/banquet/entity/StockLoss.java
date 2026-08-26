package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报损单主表。审批流程为三级：厨师长审核 -> 店长审批 -> 财务确认。
 * 主键 lossId 是业务编号（LS+日期+序号），不是自增列，创建时必须由调用方生成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_loss")
public class StockLoss {

    @Id
    @Column(name = "loss_id", length = 50)
    private String lossId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "loss_time", nullable = false)
    private LocalDateTime lossTime;

    @Column(name = "reporter_id", nullable = false)
    private Integer reporterId;

    @Column(name = "loss_type", nullable = false, length = 20)
    private String lossType;

    @Column(name = "loss_reason", length = 200)
    private String lossReason;

    @Column(name = "chef_manager_id")
    private Integer chefManagerId;

    @Column(name = "chef_manager_time")
    private LocalDateTime chefManagerTime;

    @Column(name = "chef_manager_status", length = 20)
    private String chefManagerStatus;

    @Column(name = "chef_manager_remark", length = 200)
    private String chefManagerRemark;

    @Column(name = "store_manager_id")
    private Integer storeManagerId;

    @Column(name = "store_manager_time")
    private LocalDateTime storeManagerTime;

    @Column(name = "store_manager_status", length = 20)
    private String storeManagerStatus;

    @Column(name = "store_manager_remark", length = 200)
    private String storeManagerRemark;

    @Column(name = "loss_status", length = 20)
    private String lossStatus;

    @Column(name = "total_loss_amount", precision = 12, scale = 2)
    private BigDecimal totalLossAmount;

    @Column(name = "finance_confirmed")
    private Boolean financeConfirmed;

    @Column(name = "finance_confirmed_by")
    private Integer financeConfirmedBy;

    @Column(name = "finance_confirmed_time")
    private LocalDateTime financeConfirmedTime;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    /** 生产库该列是 bigint（毫秒时间戳），不是 datetime，保持类型一致 */
    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.chefManagerStatus == null) this.chefManagerStatus = "待审核";
        if (this.storeManagerStatus == null) this.storeManagerStatus = "待审批";
        if (this.lossStatus == null) this.lossStatus = "待审核";
        if (this.financeConfirmed == null) this.financeConfirmed = false;
        if (this.isDeleted == null) this.isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
