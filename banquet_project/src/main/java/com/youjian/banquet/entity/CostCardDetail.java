package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cost_card_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostCardDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "cost_card_id")
    private Long costCardId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name")
    private String ingredientName;

    @Column(name = "category")
    private String category;

    @Column(name = "quantity", precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;
}