package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 餐桌使用
 * 来源：点餐系统 canzhuoshiyong / 表 bt_table_usage
 */
@Data
@Entity
@Table(name = "canzhuoshiyong")
public class BtTableUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canzhuohaoma", length = 200)
    private String canzhuohaoma;

    @Column(name = "canzhuoweizhi", length = 200)
    private String canzhuoweizhi;

    @Column(name = "kezuorenshu")
    private Integer kezuorenshu;

    @Column(name = "shiyongshijian")
    private LocalDateTime shiyongshijian;

    @Column(name = "yonghuming", length = 200)
    private String yonghuming;

    @Column(name = "xingming", length = 200)
    private String xingming;

    @Column(name = "shouji", length = 200)
    private String shouji;

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