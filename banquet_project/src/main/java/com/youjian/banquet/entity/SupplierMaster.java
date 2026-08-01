package com.youjian.banquet.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.youjian.banquet.config.BankAccountConverter;
import com.youjian.banquet.config.SensitiveDataSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(name = "supplier_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "supplier_code", length = 20)
    private String supplierCode;

    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "bank_account", length = 50)
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String bankAccount;

    @Column(name = "platform_account", length = 100)
    private String platformAccount;

    @Column(name = "main_products", columnDefinition = "TEXT")
    private String mainProducts;

    @Column(name = "wechat_account", length = 50)
    private String wechatAccount;

    @Column(name = "alipay_account", length = 50)
    private String alipayAccount;

    @Column(name = "taobao_account", length = 50)
    private String taobaoAccount;

    @Column(name = "supplier_rating")
    private Integer supplierRating;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "phone", length = 255)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
