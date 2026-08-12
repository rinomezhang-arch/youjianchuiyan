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
 * 参保城市实体 (HR系统复刻)
 * 对应表: hr_soc_city
 * 来源: HR系统 soc_city 表
 */
@Entity
@Table(name = "hr_soc_city")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrSocCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "average_salary", precision = 10, scale = 3)
    private BigDecimal averageSalary;

    @Column(name = "lower_salary", precision = 10, scale = 3)
    private BigDecimal lowerSalary;

    @Column(name = "soc_upper_limit", precision = 10, scale = 3)
    private BigDecimal socUpperLimit;

    @Column(name = "soc_lower_limit", precision = 10, scale = 3)
    private BigDecimal socLowerLimit;

    @Column(name = "hou_upper_limit", precision = 10, scale = 3)
    private BigDecimal houUpperLimit;

    @Column(name = "hou_lower_limit", precision = 10, scale = 3)
    private BigDecimal houLowerLimit;

    @Column(name = "per_pension_rate", precision = 6, scale = 3)
    private BigDecimal perPensionRate;

    @Column(name = "com_pension_rate", precision = 6, scale = 3)
    private BigDecimal comPensionRate;

    @Column(name = "per_medical_rate", precision = 6, scale = 3)
    private BigDecimal perMedicalRate;

    @Column(name = "com_medical_rate", precision = 6, scale = 3)
    private BigDecimal comMedicalRate;

    @Column(name = "per_unemployment_rate", precision = 6, scale = 3)
    private BigDecimal perUnemploymentRate;

    @Column(name = "com_unemployment_rate", precision = 6, scale = 3)
    private BigDecimal comUnemploymentRate;

    @Column(name = "com_maternity_rate", precision = 6, scale = 3)
    private BigDecimal comMaternityRate;

    @Column(name = "com_injury_rate", precision = 6, scale = 3)
    private BigDecimal comInjuryRate;

    @Column(name = "remark", length = 200)
    private String remark;

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