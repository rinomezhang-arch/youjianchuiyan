package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "preprocessing_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreprocessingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name")
    private String ingredientName;

    @Column(name = "raw_qty", precision = 10, scale = 3)
    private BigDecimal rawQty;

    @Column(name = "processed_qty", precision = 10, scale = 3)
    private BigDecimal processedQty;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "unit")
    private String unit;

    @Column(name = "preprocessing_type")
    private String preprocessingType;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "operator")
    private String operator;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "create_time")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}