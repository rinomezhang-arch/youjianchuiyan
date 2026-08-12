package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 配置
 * 来源：点餐系统 config / 表 bt_config
 */
@Data
@Entity
@Table(name = "config")
public class BtConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "value", length = 100)
    private String value;

    @Column(name = "store_id")
    private Long storeId;
}