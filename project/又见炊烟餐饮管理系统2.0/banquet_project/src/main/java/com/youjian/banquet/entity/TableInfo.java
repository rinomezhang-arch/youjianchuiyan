package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 餐桌信息
 * 表 table_info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "table_info")
public class TableInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "table_number", nullable = false, length = 200)
    private String tableNumber;

    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "status", length = 200)
    private String status;

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