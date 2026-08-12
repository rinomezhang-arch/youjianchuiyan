package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * HR角色菜单关系实体
 * 原表：per_role_menu → 新表：hr_role_menu
 */
@Entity
@Table(name = "hr_role_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrRoleMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "menu_id", nullable = false)
    private Integer menuId;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

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
        if (this.status == null) {
            this.status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}