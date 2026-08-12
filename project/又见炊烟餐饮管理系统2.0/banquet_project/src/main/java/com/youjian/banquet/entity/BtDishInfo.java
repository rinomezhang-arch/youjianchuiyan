package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜品信息
 * 来源：点餐系统 caipinxinxi / 表 bt_dish_info
 */
@Data
@Entity
@Table(name = "caipinxinxi")
public class BtDishInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caipinmingcheng", nullable = false, length = 200)
    private String caipinmingcheng;

    @Column(name = "caipinleixing", nullable = false, length = 200)
    private String caipinleixing;

    @Column(name = "tupian", columnDefinition = "LONGTEXT")
    private String tupian;

    @Column(name = "kouwei", length = 200)
    private String kouwei;

    @Column(name = "yujishijian", length = 200)
    private String yujishijian;

    @Column(name = "caipinjieshao", columnDefinition = "LONGTEXT")
    private String caipinjieshao;

    @Column(name = "fabushijian")
    private LocalDateTime fabushijian;

    @Column(name = "clicktime")
    private LocalDateTime clicktime;

    @Column(name = "price", nullable = false)
    private Float price;

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