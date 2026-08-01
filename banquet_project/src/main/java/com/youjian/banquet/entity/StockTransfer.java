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
@Table(name = "stock_transfer")
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "transfer_no")
    private String transferNo;

    @Column(name = "from_store_id")
    private Long fromStoreId;

    @Column(name = "to_store_id")
    private Long toStoreId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "status")
    private String status;

    @Column(name = "maker_name")
    private String makerName;

    @Column(name = "make_date")
    private LocalDate makeDate;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "草稿";
        }
        if (makeDate == null) {
            makeDate = LocalDate.now();
        }
    }
}
