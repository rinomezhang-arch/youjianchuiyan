package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_in")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseIn implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "addtime")
    private LocalDateTime addtime;

    @Column(name = "material_name", length = 200)
    private String materialName;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "in_time")
    private LocalDateTime inTime;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "supplier_account", length = 200)
    private String supplierAccount;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Transient
    private Integer status;

    @Transient
    private Float price;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
        if (this.inTime == null && this.status != null && this.status == 1) {
            this.inTime = LocalDateTime.now();
        }
    }
}