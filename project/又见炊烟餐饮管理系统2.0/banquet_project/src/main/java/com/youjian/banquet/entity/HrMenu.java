package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR菜单实体
 * 原表：per_menu → 新表：hr_menu
 * 完整保留原系统的菜单树结构（parent_id + children）
 */
@Entity
@Table(name = "hr_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "icon", length = 20)
    private String icon;

    @Column(name = "path", length = 100)
    private String path;

    @Column(name = "parent_id", nullable = false)
    private Integer parentId;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    /**
     * 子菜单列表（非数据库字段，仅用于菜单树构建）
     * 对应原系统 Menu.children
     */
    @Transient
    private List<HrMenu> children;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
        if (this.storeId == null) {
            this.storeId = 1L;
        }
        if (this.parentId == null) {
            this.parentId = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}