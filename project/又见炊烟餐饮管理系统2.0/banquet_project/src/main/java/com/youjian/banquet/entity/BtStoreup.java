package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏
 * 来源：点餐系统 storeup / 表 bt_storeup
 */
@Data
@Entity
@Table(name = "storeup")
public class BtStoreup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userid", nullable = false)
    private Long userid;

    @Column(name = "refid")
    private Long refid;

    @Column(name = "tablename", length = 200)
    private String tablename;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "picture", nullable = false, columnDefinition = "LONGTEXT")
    private String picture;

    @Column(name = "type", length = 200)
    private String type;

    @Column(name = "inteltype", length = 200)
    private String inteltype;

    @Column(name = "remark", length = 200)
    private String remark;

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