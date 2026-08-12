package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 普通用户（会员）
 * 来源：点餐系统 yonghu / 表 bt_yonghu
 */
@Data
@Entity
@Table(name = "yonghu")
public class BtYonghu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yonghuming", nullable = false, unique = true, length = 200)
    private String yonghuming;

    @Column(name = "mima", nullable = false, length = 200)
    private String mima;

    @Column(name = "xingming", nullable = false, length = 200)
    private String xingming;

    @Column(name = "touxiang", columnDefinition = "LONGTEXT")
    private String touxiang;

    @Column(name = "xingbie", length = 200)
    private String xingbie;

    @Column(name = "youxiang", length = 200)
    private String youxiang;

    @Column(name = "shouji", length = 200)
    private String shouji;

    @Column(name = "money")
    private Float money;

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