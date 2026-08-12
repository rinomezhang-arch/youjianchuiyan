package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单
 * 来源：点餐系统 orders / 表 bt_order
 */
@Data
@Entity
@Table(name = "orders")
public class BtOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderid", nullable = false, unique = true, length = 200)
    private String orderid;

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

    @Column(name = "price", nullable = false)
    private Float price;

    @Column(name = "discountprice")
    private Float discountprice;

    @Column(name = "total", nullable = false)
    private Float total;

    @Column(name = "discounttotal")
    private Float discounttotal;

    @Column(name = "type")
    private Integer type;

    @Column(name = "status", length = 200)
    private String status;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "tel", length = 200)
    private String tel;

    @Column(name = "consignee", length = 200)
    private String consignee;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "logistics", columnDefinition = "LONGTEXT")
    private String logistics;

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