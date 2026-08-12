package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "supplier_account", length = 200)
    private String supplierAccount;

    @Column(name = "password", length = 200)
    private String password;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "image", columnDefinition = "longtext")
    private String image;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;

    @Column(name = "contact_phone", length = 200)
    private String contactPhone;

    @Column(name = "supplier_address", length = 200)
    private String supplierAddress;

    @Column(name = "balance")
    private Float balance;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.balance == null) {
            this.balance = 0f;
        }
    }
}