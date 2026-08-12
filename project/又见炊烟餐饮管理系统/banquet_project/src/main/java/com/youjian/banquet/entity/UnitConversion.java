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

@Entity
@Table(name = "unit_conversion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversion_id")
    private Long conversionId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "from_unit", length = 20)
    private String fromUnit;

    @Column(name = "to_unit", length = 20)
    private String toUnit;

    @Column(name = "conversion_rate", precision = 15, scale = 6)
    private BigDecimal conversionRate;

    @Column(name = "reverse_rate", precision = 15, scale = 6)
    private BigDecimal reverseRate;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
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
