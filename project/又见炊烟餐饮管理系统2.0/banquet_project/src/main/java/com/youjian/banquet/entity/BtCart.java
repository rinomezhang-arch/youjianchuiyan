package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 购物车
 * 来源：点餐系统 cart / 表 bt_cart
 */
@Data
@Entity
@Table(name = "cart")
public class BtCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tablename", length = 200)
    private String tablename;

    @Column(name = "userid", nullable = false)
    private Long userid;

    @Column(name = "goodid", nullable = false)
    private Long goodid;

    @Column(name = "goodname", length = 200)
    private String goodname;

    @Column(name = "picture", columnDefinition = "LONGTEXT")
    private String picture;

    @Column(name = "buynumber", nullable = false)
    private Integer buynumber;

    @Column(name = "price")
    private Float price;

    @Column(name = "discountprice")
    private Float discountprice;

    @Column(name = "goodtype", length = 200)
    private String goodtype;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @PrePersist
    protected void onCreate() {
        this.addtime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
    }
}