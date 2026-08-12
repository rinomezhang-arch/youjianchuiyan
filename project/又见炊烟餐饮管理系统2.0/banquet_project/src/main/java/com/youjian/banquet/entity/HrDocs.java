package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * HR文件表（复刻自HR系统 sys_docs 表）
 * 使用 JPA 替代 MyBatis-Plus，增加 store_id 多租户支持
 *
 * @author cow
 * @since 2022-02-24
 */
@Entity
@Table(name = "hr_docs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrDocs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** 门店ID（多租户） */
    @Column(name = "store_id")
    private Long storeId;

    /** 文件名称 */
    @Column(name = "name")
    private String name;

    /** 文件类型 */
    @Column(name = "type")
    private String type;

    /** 文件原名称 */
    @Column(name = "old_name")
    private String oldName;

    /** 文件md5信息 */
    @Column(name = "md5")
    private String md5;

    /** 文件大小kB */
    @Column(name = "size")
    private Long size;

    /** 文件上传者id */
    @Column(name = "staff_id")
    private Integer staffId;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 修改时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 0未删除，1已删除，默认为0 */
    @Column(name = "is_deleted")
    private Integer isDeleted;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}