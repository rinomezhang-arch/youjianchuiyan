package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.youjian.banquet.config.BankAccountConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店信息实体
 * 对应 store_info 表
 * P2-3: bank_account 字段加密
 */
@Entity
@Table(name = "store_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreInfo {

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_code", nullable = false, length = 50)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "store_short_name", length = 50)
    private String storeShortName;

    @Column(name = "store_type", length = 20)
    private String storeType;

    @Column(name = "store_level", length = 20)
    private String storeLevel;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "business_hours", length = 100)
    private String businessHours;

    @Column(name = "table_count")
    private Integer tableCount;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "business_area", precision = 8, scale = 2)
    private BigDecimal businessArea;

    @Column(name = "manager_id")
    private Integer managerId;

    @Column(name = "manager_name", length = 50)
    private String managerName;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "tax_no", length = 50)
    private String taxNo;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account", length = 50)
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = com.youjian.banquet.config.SensitiveDataSerializer.class)
    private String bankAccount;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "store_image_url", length = 255)
    private String storeImageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "open";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
