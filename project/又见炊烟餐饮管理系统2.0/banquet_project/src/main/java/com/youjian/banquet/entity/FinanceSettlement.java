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
@Table(name = "finance_settlement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "settlement_no")
    private String settlementNo;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "settlement_type")
    private String settlementType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_income", precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", precision = 12, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "total_profit", precision = 12, scale = 2)
    private BigDecimal totalProfit;

    @Column(name = "food_cost", precision = 12, scale = 2)
    private BigDecimal foodCost;

    @Column(name = "labor_cost", precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "rent_cost", precision = 12, scale = 2)
    private BigDecimal rentCost;

    @Column(name = "utility_cost", precision = 12, scale = 2)
    private BigDecimal utilityCost;

    @Column(name = "other_cost", precision = 12, scale = 2)
    private BigDecimal otherCost;

    @Column(name = "cost_rate", precision = 5, scale = 2)
    private BigDecimal costRate;

    @Column(name = "status")
    private String status;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
