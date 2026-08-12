package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 购物车
 * 表 dish_cart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dish_cart")
public class DishCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "good_id")
    private Long goodId;

    @Column(name = "good_name", length = 200)
    private String goodName;

    @Column(name = "picture", columnDefinition = "LONGTEXT")
    private String picture;

    @Column(name = "buy_number")
    private Integer buyNumber;

    @Column(name = "price")
    private Float price;

    @Column(name = "discount_price")
    private Float discountPrice;

    @Column(name = "good_type", length = 200)
    private String goodType;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
    }
}