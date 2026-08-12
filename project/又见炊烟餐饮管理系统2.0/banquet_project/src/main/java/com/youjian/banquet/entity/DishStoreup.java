package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 收藏
 * 表 dish_storeup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dish_storeup")
public class DishStoreup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "picture", columnDefinition = "LONGTEXT")
    private String picture;

    @Column(name = "type", length = 200)
    private String type;

    @Column(name = "intel_type", length = 200)
    private String intelType;

    @Column(name = "remark", length = 200)
    private String remark;

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