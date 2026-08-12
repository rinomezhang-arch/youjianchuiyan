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
 * 员工五险一金实体 (HR系统复刻)
 * 对应表: hr_soc_insurance
 * 来源: HR系统 soc_insurance 表
 */
@Entity
@Table(name = "hr_soc_insurance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrSocInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "house_base", precision = 10, scale = 3)
    private BigDecimal houseBase;

    @Column(name = "per_house_rate", precision = 6, scale = 3)
    private BigDecimal perHouseRate;

    @Column(name = "per_house_pay", precision = 10, scale = 3)
    private BigDecimal perHousePay;

    @Column(name = "com_house_rate", precision = 6, scale = 3)
    private BigDecimal comHouseRate;

    @Column(name = "com_house_pay", precision = 10, scale = 3)
    private BigDecimal comHousePay;

    @Column(name = "social_base", precision = 10, scale = 3)
    private BigDecimal socialBase;

    @Column(name = "com_social_pay", precision = 10, scale = 3)
    private BigDecimal comSocialPay;

    @Column(name = "per_social_pay", precision = 10, scale = 3)
    private BigDecimal perSocialPay;

    @Column(name = "com_injury_rate", precision = 6, scale = 3)
    private BigDecimal comInjuryRate;

    @Column(name = "social_remark", length = 200)
    private String socialRemark;

    @Column(name = "house_remark", length = 200)
    private String houseRemark;

    @Column(name = "pay_month", length = 6)
    private String payMonth;

    @Column(name = "status")
    private Integer status;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}