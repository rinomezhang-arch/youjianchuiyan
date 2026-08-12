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
@Table(name = "material_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialInfo implements Serializable {

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

    @Column(name = "image", columnDefinition = "longtext")
    private String image;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "specification", length = 200)
    private String specification;

    @Column(name = "detail", columnDefinition = "longtext")
    private String detail;

    @Column(name = "supplier_account", length = 200)
    private String supplierAccount;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "single_limit")
    private Integer singleLimit;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "click_time")
    private LocalDateTime clickTime;

    @Column(name = "price")
    private Float price;

    @PrePersist
    protected void onCreate() {
        if (this.addtime == null) {
            this.addtime = LocalDateTime.now();
        }
    }
}