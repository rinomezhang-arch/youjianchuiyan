package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 餐桌使用记录
 * 表 table_usage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "table_usage")
public class TableUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "table_number", length = 200)
    private String tableNumber;

    @Column(name = "table_location", length = 200)
    private String tableLocation;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "use_time")
    private LocalDateTime useTime;

    @Column(name = "username", length = 200)
    private String username;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "phone", length = 200)
    private String phone;

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