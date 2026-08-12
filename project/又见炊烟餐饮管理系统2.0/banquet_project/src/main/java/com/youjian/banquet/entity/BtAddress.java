package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 地址
 * 来源：点餐系统 address / 表 bt_address
 */
@Data
@Entity
@Table(name = "address")
public class BtAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userid", nullable = false)
    private Long userid;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", nullable = false, length = 200)
    private String phone;

    @Column(name = "isdefault", nullable = false, length = 200)
    private String isdefault;

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