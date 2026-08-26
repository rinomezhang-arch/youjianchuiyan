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
import java.time.LocalDateTime;

/**
 * 奖惩记录表
 * 对应规划手册 5.txt 阶段2.3
 */
@Entity
@Table(name = "reward_punish")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardPunish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rp_id")
    private Long rpId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "rp_no", nullable = false, length = 30)
    private String rpNo;

    @Column(name = "rp_type", nullable = false)
    private Integer rpType;

    @Column(name = "rp_category", nullable = false, length = 50)
    private String rpCategory;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_path", length = 500)
    private String evidencePath;

    @Column(name = "approver_1_id")
    private Long approver1Id;

    @Column(name = "approver_1_status")
    private Integer approver1Status = 1;

    @Column(name = "approver_1_time")
    private LocalDateTime approver1Time;

    @Column(name = "approver_1_remark", length = 255)
    private String approver1Remark;

    @Column(name = "approver_2_id")
    private Long approver2Id;

    @Column(name = "approver_2_status")
    private Integer approver2Status = 1;

    @Column(name = "approver_2_time")
    private LocalDateTime approver2Time;

    @Column(name = "approver_2_remark", length = 255)
    private String approver2Remark;

    @Column(name = "final_status")
    private Integer finalStatus = 1;

    @Column(name = "is_synced_to_salary")
    private Integer isSyncedToSalary = 0;

    @Column(name = "sync_salary_id")
    private Long syncSalaryId;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
