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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR员工表（复刻自HR系统 sys_staff 表）
 * 使用 JPA 替代 MyBatis-Plus，增加 store_id 多租户支持
 *
 * @author cow
 * @since 2022-01-27
 */
@Entity
@Table(name = "hr_staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** 门店ID（多租户） */
    @Column(name = "store_id")
    private Long storeId;

    /** 员工编码 */
    @Column(name = "code")
    private String code;

    /** 员工姓名 */
    @Column(name = "name")
    private String name;

    /** 性别，0男，1女，默认男 */
    @Column(name = "gender")
    private Integer gender;

    /** 员工密码 */
    @Column(name = "pwd")
    private String password;

    /** 员工头像 */
    @Column(name = "avatar")
    private String avatar;

    /** 员工生日 */
    @Column(name = "birthday")
    private LocalDate birthday;

    /** 员工电话 */
    @Column(name = "phone")
    private String phone;

    /** 地址 */
    @Column(name = "address")
    private String address;

    /** 员工备注 */
    @Column(name = "remark")
    private String remark;

    /** 部门id */
    @Column(name = "dept_id")
    private Integer deptId;

    /** 员工状态，0离职，1在职，2禁用 */
    @Column(name = "status")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除，0未删除，1删除 */
    @Column(name = "is_deleted")
    private Integer isDeleted;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
        if (this.status == null) {
            this.status = 1;
        }
        if (this.gender == null) {
            this.gender = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}