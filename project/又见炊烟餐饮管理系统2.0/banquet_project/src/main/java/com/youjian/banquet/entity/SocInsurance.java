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
 * 五险一金表
 * 对应数据库表: soc_insurance
 */
@Entity
@Table(name = "soc_insurance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "house_base", precision = 10, scale = 3)
    private BigDecimal houseBase;

    @Column(name = "per_house_rate", precision = 10, scale = 3)
    private BigDecimal perHouseRate;

    @Column(name = "per_house_pay", precision = 10, scale = 3)
    private BigDecimal perHousePay;

    @Column(name = "com_house_rate", precision = 10, scale = 3)
    private BigDecimal comHouseRate;

    @Column(name = "com_house_pay", precision = 10, scale = 3)
    private BigDecimal comHousePay;

    @Column(name = "social_base", precision = 10, scale = 3)
    private BigDecimal socialBase;

    @Column(name = "com_social_pay", precision = 10, scale = 3)
    private BigDecimal comSocialPay;

    @Column(name = "per_social_pay", precision = 10, scale = 3)
    private BigDecimal perSocialPay;

    @Column(name = "com_injury_rate", precision = 10, scale = 3)
    private BigDecimal comInjuryRate;

    @Column(name = "social_remark", length = 200)
    private String socialRemark;

    @Column(name = "house_remark", length = 200)
    private String houseRemark;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
        if (this.status == null) {
            this.status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}