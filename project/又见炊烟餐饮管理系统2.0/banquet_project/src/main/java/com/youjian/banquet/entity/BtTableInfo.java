package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 餐桌信息
 * 来源：点餐系统 canzhuoxinxi / 表 bt_table_info
 */
@Data
@Entity
@Table(name = "canzhuoxinxi")
public class BtTableInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canzhuohaoma", nullable = false, unique = true, length = 200)
    private String canzhuohaoma;

    @Column(name = "tupian", columnDefinition = "LONGTEXT")
    private String tupian;

    @Column(name = "kezuorenshu")
    private Integer kezuorenshu;

    @Column(name = "canzhuoweizhi", length = 200)
    private String canzhuoweizhi;

    @Column(name = "canzhuozhuangtai", length = 200)
    private String canzhuozhuangtai;

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